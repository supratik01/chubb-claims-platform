import { loadRemoteModule } from '@angular-architects/native-federation';
import { Routes } from '@angular/router';
import { RemoteUnavailablePage } from './pages/remote-unavailable.page';

/**
 * Loads a micro-frontend's routes at runtime. Its address comes from federation.manifest.json, so it can be
 * redeployed without rebuilding the shell. If it cannot be loaded, the user sees an explanation instead of a
 * blank page, and the rest of the shell keeps working.
 */
export function loadRemoteRoutes(remoteName: 'claimant' | 'staff'): () => Promise<Routes> {
  return () =>
    loadRemoteModule<{ routes: Routes }>(remoteName, './routes')
      .then((remote) => remote.routes)
      .catch((error: unknown) => {
        console.error(`Could not load the ${remoteName} micro-frontend`, error);
        return [{ path: '**', component: RemoteUnavailablePage }];
      });
}
