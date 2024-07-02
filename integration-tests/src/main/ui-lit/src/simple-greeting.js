import {html, css, LitElement} from 'lit';

const API_PATH = process.env.API_PATH;

export class SimpleGreeting extends LitElement {
    static styles = css`p { color: blue }`;

    static properties = {
        name: {type: String},
    };

    constructor() {
        super();
        this.name = 'Somebody';
    }

    render() {
        const xmlHttp = new XMLHttpRequest();

        xmlHttp.open( "GET", `${API_PATH}quinoa`, false );
        xmlHttp.send( null );
        const response = xmlHttp.responseText;
        return html`<p class="greeting">${response} and ${this.name} and ${process.env.FOO}</p>`;
    }
}
customElements.define('simple-greeting', SimpleGreeting);
