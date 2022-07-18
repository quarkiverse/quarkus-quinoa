import {html, css, LitElement} from 'lit';

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
        var xmlHttp = new XMLHttpRequest();
        xmlHttp.open( "GET", "/api/quinoa", false );
        xmlHttp.send( null );
        const response = xmlHttp.responseText;
        return html`<p class="greeting">${response} and ${this.name} and ${process.env.FOO}</p>`;
    }
}
customElements.define('simple-greeting', SimpleGreeting);
