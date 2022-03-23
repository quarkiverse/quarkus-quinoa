import { useEffect, useState } from "react";
import logo from './logo.svg';
import './App.css';

function App() {
  const [value, setValue] = useState(null);
  useEffect(async () => {
    if(value == null) {
      const result = await fetch(
          '/api/quinoa',
      );
      let text = await result.text();
      setValue(text);
    }
  }, [value, setValue]);
  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <p>
          Edit <code>src/App.js</code> and save to reload.
        </p>
        <p>
          {value || "loading from api..."}
        </p>
        <a
          className="App-link"
          href="https://reactjs.org"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn React
        </a>
      </header>
    </div>
  );
}

export default App;
