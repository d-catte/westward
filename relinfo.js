async function makeDownloadButtons() {
    const res = await fetch("https://api.github.com/repos/d-catte/westward/releases/latest");
    const release = await res.json();
    const urlBase = `https://github.com/d-catte/westward/releases/download/${release.tag_name}`;

    const h2 = document.createElement('h2');
    h2.innerText = `Download ${release.name}`;
    document.getElementById("download").before(h2);

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

    if (release.assets.some(asset => asset.name === `westward-${release.tag_name}.jar`)) {
        document.getElementById("jar").setAttribute("href", `${urlBase}/westward-${release.tag_name}.jar`);
    }
}