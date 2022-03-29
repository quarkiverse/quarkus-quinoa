import { useEffect, useState } from "react";
import logo from './logo.svg';
import './App.css';

async function fetchQuinoa(setValue) {
  let response = await fetch('api/quinoa')
  response = await response.text()
  setValue(response)
}

function App() {
  const [value, setValue] = useState(null);
  useEffect(() => {
    if(value == null) {
      fetchQuinoa(setValue);
    }
  }, [value, setValue]);
  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <p>
          Edit <code>src/App.js</code> and save to reload.
        </p>
        <p className="quinoa">
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
