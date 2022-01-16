import React from 'react';
import { Navbar, Heading, Section, Message } from 'react-bulma-components';
import { Route, Routes, Link } from "react-router-dom";
import {ErrorBoundary, FallbackProps} from 'react-error-boundary';

import './App.css';
import 'bulma/css/bulma.min.css';
import ScenarioPage from "./pages/ScenarioPage";

function App() {
  return (
    <div className="App">
    <ErrorBoundary fallbackRender={renderError}>
      <Header/>
      <Section>
        <Routes>
            <Route path="/" element={<ScenarioPage />} />
        </Routes>
      </Section>
    </ErrorBoundary>
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

function renderError(props: FallbackProps) {
    return (
        <Message className={"is-danger"}>
            <Message.Header>Something went wrong</Message.Header>
            <Message.Body>
                <pre>{props.error.message}</pre>
                <pre>{props.error.stack}</pre>
            </Message.Body>
        </Message>
    )
}

export default App;
