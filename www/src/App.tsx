import React from 'react';
import { Navbar, Heading, Section } from 'react-bulma-components';
import { Route, Routes, Link } from "react-router-dom";

import './App.css';
import 'bulma/css/bulma.min.css';
import ScenarioPage from "./pages/ScenarioPage";

function App() {
  return (
    <div className="App">
      <Header/>
      <Section>
        <Routes>
            <Route path="/" element={<ScenarioPage />} />
        </Routes>
      </Section>
    </div>
  );
}

function Header() {
  return (
    <Navbar className="is-info">
      <Navbar.Brand id={"m-logo"}>
        <Navbar.Item hoverable={false}>
          <Heading size={3} spaced={true}>
            Deep-Trace
          </Heading>
        </Navbar.Item>
      </Navbar.Brand>

      <Navbar.Menu>
        <Navbar.Container align="left">
          <Navbar.Item>Test</Navbar.Item>
          <Navbar.Item>Test1</Navbar.Item>
          <Navbar.Item>Test2</Navbar.Item>
          <Navbar.Item>Test3</Navbar.Item>
        </Navbar.Container>
      </Navbar.Menu>
    </Navbar>
  );
}

export default App;
