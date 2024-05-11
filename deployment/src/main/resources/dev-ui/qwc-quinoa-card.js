import { LitElement, html, css} from 'lit';
import { pages } from 'build-time-data';
import 'qwc/qwc-extension-link.js';

export class QwcQuinoaCard extends LitElement {

    static styles = css`
      .identity {
        display: flex;
        justify-content: flex-start;
      }

      .description {
        padding-bottom: 10px;
      }

      .logo {
        padding-bottom: 10px;
        margin-right: 5px;
      }

      .card-content {
        color: var(--lumo-contrast-90pct);
        display: flex;
        flex-direction: column;
        justify-content: flex-start;
        padding: 2px 2px;
        height: 100%;
      }

      .card-content slot {
        display: flex;
        flex-flow: column wrap;
        padding-top: 5px;
      }
    `;

    static properties = {
        extensionName: {attribute: true},
        description: {attribute: true},
        guide: {attribute: true},
        namespace: {attribute: true},
    };

    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback();
    }

    render() {
        return html`<div class="card-content" slot="content">
            <div class="identity">
                <div class="logo">
                    <img src="data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4NCjwhRE9DVFlQRSBzdmcgUFVCTElDICItLy9XM0MvL0RURCBTVkcgMS4xLy9FTiIgImh0dHA6Ly93d3cudzMub3JnL0dyYXBoaWNzL1NWRy8xLjEvRFREL3N2ZzExLmR0ZCI+DQo8c3ZnIHZlcnNpb249IjEuMSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4PSIwcHgiIHk9IjBweCIgd2lkdGg9IjU0MHB4IiBoZWlnaHQ9IjIxMHB4IiB2aWV3Qm94PSIwIDAgMTggNyI+DQo8cGF0aCBmaWxsPSIjQ0IzODM3IiBkPSJNMCwwaDE4djZIOXYxSDVWNkgwVjB6IE0xLDVoMlYyaDF2M2gxVjFIMVY1eiBNNiwxdjVoMlY1aDJWMUg2eiBNOCwyaDF2Mkg4VjJ6IE0xMSwxdjRoMlYyaDF2M2gxVjJoMXYzaDFWMUgxMXoiLz4NCjxwb2x5Z29uIGZpbGw9IiNGRkZGRkYiIHBvaW50cz0iMSw1IDMsNSAzLDIgNCwyIDQsNSA1LDUgNSwxIDEsMSAiLz4NCjxwYXRoIGZpbGw9IiNGRkZGRkYiIGQ9Ik02LDF2NWgyVjVoMlYxSDZ6IE05LDRIOFYyaDFWNHoiLz4NCjxwb2x5Z29uIGZpbGw9IiNGRkZGRkYiIHBvaW50cz0iMTEsMSAxMSw1IDEzLDUgMTMsMiAxNCwyIDE0LDUgMTUsNSAxNSwyIDE2LDIgMTYsNSAxNyw1IDE3LDEgIi8+DQo8L3N2Zz4NCg=="
                                       alt="${this.extensionName}" 
                                       title="${this.extensionName}"
                                       width="64" 
                                       height="32">
                </div>
                <div class="description">${this.description}</div>
            </div>
            ${this._renderCardLinks()}
        </div>
        `;
    }

    _renderCardLinks(){
        return html`${pages.map(page => html`
                            <qwc-extension-link slot="link"
                                namespace="${this.namespace}"
                                extensionName="${this.extensionName}"
                                iconName="${page.icon}"
                                displayName="${page.title}"
                                staticLabel="${page.staticLabel}"
                                dynamicLabel="${page.dynamicLabel}"
                                streamingLabel="${page.streamingLabel}"
                                path="${page.id}"
                                ?embed=${page.embed}
                                externalUrl="${page.metadata.externalUrl}"
                                webcomponent="${page.componentLink}" >
                            </qwc-extension-link>
                        `)}`;
    }

}
customElements.define('qwc-quinoa-card', QwcQuinoaCard);