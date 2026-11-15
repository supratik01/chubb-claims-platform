import {
  withNativeFederation,
  fromPackageJson,
  mappingsFromWorkspace,
} from '@angular-architects/native-federation/config';

export default withNativeFederation({
  name: 'claimant',

  // The shell mounts these routes under /claimant; nothing else is public.
  exposes: {
    './routes': './projects/claimant/src/app/claimant.routes.ts',
  },

  // Angular, RxJS and the other npm dependencies are loaded once and shared by the shell and both micro-frontends.
  shared: fromPackageJson({
    singleton: true,
    strictVersion: true,
    requiredVersion: 'auto',
    build: 'package',
  }).patch(['@angular/*'], { includeSecondaries: { keepAll: true } }),

  // The workspace library is shared as a singleton too, so the session store, the HTTP client wrapper and the theme
  // components exist once at runtime and the shell's sign-in is visible to both micro-frontends.
  sharedMappings: mappingsFromWorkspace({ singleton: true }).filter(['@claims/shared']).get(),

  skip: ['rxjs/ajax', 'rxjs/fetch', 'rxjs/testing', 'rxjs/webSocket'],

  features: {
    denseChunking: true,
    denseExternals: true,
    strictImports: false,
  },
});
