let currentFolder = "";

const uploadButton = document.getElementById("uploadButton");
const fileInput = document.getElementById("fileInput");
const newFolderButton = document.getElementById("newFolderButton");
const fileGrid = document.getElementById("fileGrid");
const folderDirectory = document.getElementById("folderDirectory");
const recentFilesBody = document.getElementById("recentFilesBody");
const filesHeading = document.getElementById("filesH2");

// Sends a request to Java and read the JSON response
async function request(url, options = {}) {
    const response = await fetch(url, options);

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));

        throw new Error(
            error.message || error.detail || `Request failed: ${response.status}`
        );
    }

    if (response.status === 204) {
        return;
    }

    return response.json();
}

// Convert bytes into readable data
function formatSize(bytes) {
    if (bytes < 1024) {
        return `${bytes} B`;
    }

    if (bytes < 1024 * 1024) {
        return `${(bytes / 1024).toFixed(1)} KB`;
    }

    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// Opens the local device storage to upload files
uploadButton.addEventListener("click", () => {
    fileInput.click();
});

// Upload after the user selects files
fileInput.addEventListener("change", async () => {
    if (fileInput.files.length === 0) {
        return;
    }

    const formData = new FormData();

    // The name "files" matches @RequestParam("files") in Java
    for (const file of fileInput.files) {
        formData.append("files", file);
    }

    // Tell Java which folder should receive the files
    formData.append("folder", currentFolder);

    uploadButton.disabled = true;

    try {
        await request("/api/files/upload", {
            method: "POST",
            body: formData
        });

    } catch (error) {
        alert(error.message);
    } finally {
        uploadButton.disabled = false;

        fileInput.value = "";

        await refreshView();
    }
});

newFolderButton.addEventListener("click", async () => {
    const name = prompt("Enter the new folder name:");

    // null means the user clicked Cancel.
    if (name === null || name.trim() === "") {
        return;
    }

    try {
        await request("/api/files/folders", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                name: name.trim(),
                parentFolder: currentFolder
            })
        });

        await refreshView();
    } catch (error) {
        alert(error.message);
    }
});

async function openFolder(path) {
    currentFolder = path;
    await refreshView();
}

// Display files and folders inside the current folder
async function loadFiles() {
    const items = await request(
        `/api/files?folder=${encodeURIComponent(currentFolder)}`
    );

    filesHeading.textContent = currentFolder || "My Drive";
    fileGrid.replaceChildren();

    for (const item of items) {
        // Outer container for the name, size and menu.
        const card = document.createElement("div");
        card.className =
            "flex items-start gap-3 border rounded-xl p-4";

        // Folders are clickable; ordinary files are text for now.
        const content = document.createElement(
            item.directory ? "button" : "div"
        );

        content.className = "flex-1 min-w-0 text-left";

        const name = document.createElement("p");
        name.className = "break-words";
        name.textContent = item.name;

        const details = document.createElement("p");
        details.className = "text-sm text-gray-500";
        details.textContent = item.directory
            ? "Folder"
            : formatSize(item.size);

        content.append(name, details);

        if (item.directory) {
            content.type = "button";
            content.addEventListener("click", () => {
                openFolder(item.path);
            });
        }

        // A native HTML expandable menu.
        const menu = document.createElement("details");
        menu.className = "relative shrink-0";

        const menuButton = document.createElement("summary");

        // Unicode escape avoids encoding issues with the three-dot symbol.
        menuButton.textContent = "\u22EE";
        menuButton.className =
            "cursor-pointer list-none rounded px-3 py-1 " +
            "text-xl hover:bg-gray-100";

        menuButton.setAttribute(
            "aria-label",
            `Options for ${item.name}`
        );

        const menuOptions = document.createElement("div");
        menuOptions.className =
            "absolute right-0 top-full z-10 mt-1 min-w-32 " +
            "rounded-lg border bg-white p-1 shadow-lg";

        const deleteButton = document.createElement("button");
        deleteButton.type = "button";
        deleteButton.textContent = "Delete";
        deleteButton.className =
            "w-full rounded px-3 py-2 text-left " +
            "text-red-600 hover:bg-red-50";

        deleteButton.addEventListener("click", () => {
            menu.open = false;
            deleteItem(item);
        });

        menuOptions.append(deleteButton);
        menu.append(menuButton, menuOptions);

        card.append(content, menu);
        fileGrid.append(card);
    }

    if (items.length === 0) {
        const message = document.createElement("p");
        message.textContent = "This folder is empty.";
        fileGrid.append(message);
    }
}

// Build the sidebar using all folders returned by Java
async function loadFolders() {
    const folders = await request("/api/files/folders");

    folderDirectory.replaceChildren();

    // Add a button for returning to the main drive.
    addSidebarFolder("My Drive", "", folderDirectory);

    // Start displaying folders from the main drive.
    showSubfolders("", folderDirectory, folders);
}

function showSubfolders(parentPath, container, folders) {
    // Find folders directly inside this parent
    const children = folders.filter(
        folder => folder.parentFolder === parentPath
    );

    for (const folder of children) {
        const item = addSidebarFolder(
            folder.name,
            folder.path,
            container
        );

        // Create an indented list for this foldes subfolders
        const sublist = document.createElement("ul");
        sublist.className = "ml-4 border-l pl-3";

        item.append(sublist);

        // Repeat for the folders inside this folder.
        showSubfolders(folder.path, sublist, folders);
    }
}

function addSidebarFolder(label, path, container) {
    const item = document.createElement("li");
    const button = document.createElement("button");

    button.type = "button";
    button.textContent = label;
    button.className =
        "w-full py-1 text-left break-words hover:underline";

    button.addEventListener("click", () => openFolder(path));

    item.append(button);
    container.append(item);

    return item;
}

// Fill the Recent Files table
async function loadRecentFiles() {
    const files = await request("/api/files/recent");

    recentFilesBody.replaceChildren();

    for (const file of files) {
        const row = document.createElement("tr");

        const values = [
            file.name,
            new Date(file.createdAt).toLocaleString(),
            file.parentFolder || "My Drive",
            formatSize(file.size)
        ];

        for (const value of values) {
            const cell = document.createElement("td");
            cell.className = "p-3 border-b";
            cell.textContent = value;
            row.append(cell);
        }

        recentFilesBody.append(row);
    }
}

// Update all three sections
async function refreshView() {
    try {
        await loadFiles();
        await loadFolders();
        await loadRecentFiles();
    } catch (error) {
        alert(error.message);
    }
}

async function deleteItem(item) {
    const confirmed = confirm(
        `Permanently delete "${item.name}"? This cannot be undone.`
    );

    if (!confirmed) {
        return;
    }

    try {
        await request(
            `/api/files?path=${encodeURIComponent(item.path)}`,
            { method: "DELETE" }
        );

        // Update the grid, sidebar and recent files table.
        await refreshView();
    } catch (error) {
        alert(error.message);
    }
}

// Load existing files and folders when the page first opens
refreshView();