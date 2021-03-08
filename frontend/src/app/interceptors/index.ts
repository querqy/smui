import { HTTP_INTERCEPTORS } from '@angular/common/http';

import { HttpAuthInterceptor } from './http-auth-interceptor';

/** Http interceptor providers in outside-in order */
export const httpInterceptorProviders = [
  { provide: HTTP_INTERCEPTORS, useClass: HttpAuthInterceptor, multi: true }
];
