import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { apiInterceptor } from './core/api/api.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    // Chamadas relativas a /api/v1 na mesma origem; o cookie de sessão segue automaticamente.
    provideHttpClient(withFetch(), withInterceptors([apiInterceptor])),
  ],
};
