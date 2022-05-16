import _ from 'lodash';

function component() {
    const element = document.createElement('h1');

    // Lodash, now imported by this script
    element.innerHTML = _.join(['<b>Q</b>uarkus', '<b>UI</b>', '<b>NO</b>', 'h<b>A</b>ssles'], ' ');

    return element;
}

document.body.appendChild(component());
