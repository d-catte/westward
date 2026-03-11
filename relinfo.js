async function makeDownloadButtons() {
    const res = await fetch("https://api.github.com/repos/d-catte/Westward/releases/latest");
    const release = await res.json();
    const urlBase = `https://github.com/d-catte/Westward/releases/download/${release.tag_name}`;

    const div = document.getElementById("download");
    div.innerHTML = "<h1>Launchers</h1>";
    const launcherList = document.createElement('ul');

    // Only make links to launchers that exist in this release
    if (release.assets.some(asset => asset.name === "westward-launcher-windows.exe")) {
        const li = document.createElement('li');
        li.innerHTML = `<a href="${urlBase}/westward-launcher-windows.exe">Windows</a>`;
        launcherList.appendChild(li);
    }
    if (release.assets.some(asset => asset.name === "westward-launcher-macos-arm.dmg")) {
        const li = document.createElement('li');
        li.innerHTML = `<a href="${urlBase}/westward-launcher-macos-arm.dmg">macOS (Apple Silicon)</a>`;
        launcherList.appendChild(li);
    }
    if (release.assets.some(asset => asset.name === "westward-launcher-macos-intel.dmg")) {
        const li = document.createElement('li');
        li.innerHTML = `<li><a href="${urlBase}/westward-launcher-macos-intel.dmg">macOS (Intel)</a></li>`;
        launcherList.appendChild(li);
    }
    if (release.assets.some(asset => asset.name === "westward-launcher-ubuntu")) {
        const li = document.createElement('li');
        li.innerHTML = `<li><a href="${urlBase}/westward-launcher-ubuntu">Linux (x86_64)</a></li>`;
        launcherList.appendChild(li);
    }

    div.appendChild(launcherList);
    const nativeHeading = document.createElement('h1');
    nativeHeading.innerText = "Natives";
    div.appendChild(nativeHeading);
    const nativeList = document.createElement('ul');

    // Only make links to natives that exist in this release
    if (release.assets.some(asset => asset.name === `westward-windows-${release.tag_name}.exe`)) {
        const li = document.createElement('li');
        li.innerHTML = `<a href="${urlBase}/westward-windows-${release.tag_name}.exe">Windows</a>`;
        nativeList.appendChild(li);
    }
    if (release.assets.some(asset => asset.name === `westward-macos-a-${release.tag_name}`)) {
        const li = document.createElement('li');
        li.innerHTML = `<li><a href="${urlBase}/westward-macos-a-${release.tag_name}">macOS (Apple Silicon)</a></li>`;
        nativeList.appendChild(li);
    }
    if (release.assets.some(asset => asset.name === `westward-macos-i-${release.tag_name}`)) {
        const li = document.createElement('li');
        li.innerHTML = `<li><a href="${urlBase}/westward-macos-i-${release.tag_name}">macOS (Intel)</a></li>`;
        nativeList.appendChild(li);
    }
    if (release.assets.some(asset => asset.name === `westward-linux-${release.tag_name}`)) {
        const li = document.createElement('li');
        li.innerHTML = `<li><a href="${urlBase}/westward-linux-${release.tag_name}">Linux (x86_64)</a></li>`;
        nativeList.appendChild(li);
    }

    div.appendChild(nativeList);
}