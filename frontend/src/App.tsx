import type { ReactNode } from 'react';
import { BrowserRouter, HashRouter } from 'react-router-dom';
import { Capacitor } from '@capacitor/core';
import { AppRouter } from './router';

/**
 * Capacitor WebView often works best with HashRouter for file/https app schemes.
 * Browser dev uses BrowserRouter for clean URLs.
 */
function Router({ children }: { children: ReactNode }) {
  if (Capacitor.isNativePlatform()) {
    return <HashRouter>{children}</HashRouter>;
  }
  return <BrowserRouter>{children}</BrowserRouter>;
}

export default function App() {
  return (
    <Router>
      <AppRouter />
    </Router>
  );
}
