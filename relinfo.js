async function makeDownloadButtons() {
    const res = await fetch("https://api.github.com/repos/d-catte/Westward/releases/latest");
    const release = await res.json();
    const urlBase = `https://github.com/d-catte/Westward/releases/download/${release.tag_name}`;

    const windowsList = document.createElement('ul');
    if (release.assets.some(asset => asset.name === "westward-launcher-windows.exe")) {
        const a = document.createElement('a');
        a.setAttribute("href", `${urlBase}/westward-launcher-windows.exe`);
        a.innerHTML = `<li>Launcher</li>`;
        windowsList.appendChild(a);
    }
    if (release.assets.some(asset => asset.name === `westward-windows-${release.tag_name}.exe`)) {
        const a = document.createElement('a');
        a.setAttribute("href", `${urlBase}/westward-windows-${release.tag_name}.exe`);
        a.innerHTML = `<li>Standalone</li>`;
        windowsList.appendChild(a);
    }
    document.getElementById("windows").appendChild(windowsList);

    const linuxList = document.createElement('ul');
    if (release.assets.some(asset => asset.name === "westward-launcher-ubuntu")) {
        const a = document.createElement('a');
        a.setAttribute("href", `${urlBase}/westward-launcher-ubuntu`);
        a.innerHTML = `<li>Launcher</li>`;
        linuxList.appendChild(a);
    }
    if (release.assets.some(asset => asset.name === `westward-linux-${release.tag_name}`)) {
        const a = document.createElement('a');
        a.setAttribute("href", `${urlBase}/westward-linux-${release.tag_name}`);
        a.innerHTML = `<li>Standalone</li>`;
        linuxList.appendChild(a);
    }
    document.getElementById("linux").appendChild(linuxList);

    const macList = document.createElement('ul');
    if (release.assets.some(asset => asset.name === "westward-launcher-macos-arm.dmg")) {
        const a = document.createElement('a');
        a.setAttribute("href", `${urlBase}/westward-launcher-macos-arm.dmg`);
        a.innerHTML = `<li>Apple Silicon</li>`;
        macList.appendChild(a);
    }
    if (release.assets.some(asset => asset.name === "westward-launcher-macos-intel.dmg")) {
        const a = document.createElement('a');
        a.setAttribute("href", `${urlBase}/westward-launcher-macos-intel.dmg`);
        a.innerHTML = `<li>Intel Macs</li>`;
        macList.appendChild(a);
    }
    document.getElementById("macos").appendChild(macList);
}