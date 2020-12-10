import {Injectable} from '@angular/core';
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {EMPTY, Observable, throwError} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {AuthViolation} from "../models";
import 'rxjs/add/observable/empty';

@Injectable({
  providedIn: 'root'
})
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req)
      .pipe(
        catchError((error: HttpErrorResponse) => {
          if(error.status === 401) {
            try {
              console.log(':: err = ' + JSON.stringify(error.message));
              const smuiAuthViolation = error.error as AuthViolation;
              console.log(':: smuiAuthViolation = ' + JSON.stringify(smuiAuthViolation));
              if (smuiAuthViolation.action === 'redirect') {
                // TODO solve with proper angular2 imports, and not by using plain window-object
                window.location.href = ((smuiAuthViolation.params.indexOf('{{CURRENT_SMUI_URL}}') !== -1)
                  ? smuiAuthViolation.params.replace('{{CURRENT_SMUI_URL}}', encodeURI(window.location.href))
                  : smuiAuthViolation.params);
              } else {
                console.log(':: No known action found while evaluating SmuiAuthViolation JSON');
              }
            } catch (e) {
              console.log(':: error while parsing SmuiAuthViolation JSON: ' + e);
            }
            return EMPTY;
          }
          return throwError(error);
        })
      )
  }
}

//
// import { Injectable } from '@angular/core';
// import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
// import {Observable} from "rxjs";
// import { catchError } from 'rxjs/operators'
//
// // import 'rxjs/add/operator/catch';
// // import 'rxjs/add/observable/empty';
// // import 'rxjs/add/observable/throw';
//
//
// class SmuiAuthViolation {
//   action: string; // supports 'redirect' currently
//   params: string; // in case of 'redirect', param contains the redirect target (absolute URL)
// }
//
// // Implementation adopted from: https://www.illucit.com/angular/angular2-http-authentication-interceptor/
//
// @Injectable()
// export class HttpAuthInterceptor implements HttpInterceptor {
//
//   constructor(backend: ConnectionBackend, defaultOptions: RequestOptions) {
//     super(backend, defaultOptions);
//   }
//
//   request(url: string | Request, options?: RequestOptionsArgs): Observable<Response> {
//     return this.intercept(super.request(url, options));
//   }
//
//   get(url: string, options?: RequestOptionsArgs): Observable<Response> {
//     console.log('In HttpAuthInterceptor :: get');
//     return this.intercept(super.get(url, options));
//   }
//
//   post(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
//     return this.intercept(super.post(url, body, this.getRequestOptionArgs(options)));
//   }
//
//   put(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
//     return this.intercept(super.put(url, body, this.getRequestOptionArgs(options)));
//   }
//
//   delete(url: string, options?: RequestOptionsArgs): Observable<Response> {
//     return this.intercept(super.delete(url, options));
//   }
//
//   getRequestOptionArgs(options?: RequestOptionsArgs): RequestOptionsArgs {
//     console.log('In HttpAuthInterceptor :: getRequestOptionArgs');
//     if (options == null) {
//       options = new RequestOptions();
//     }
//     if (options.headers == null) {
//       options.headers = new Headers();
//     }
//     // TODO evaluate, if central definition of Content-Type makes sense
//     /*
//     options.headers.append('Content-Type', 'application/json');
//     */
//     return options;
//   }
//
//   intercept(observable: Observable<Response>): Observable<Response> {
//     console.log('In HttpAuthInterceptor :: intercept');
//     return observable.catch((err, source) => {
//       if (err.status === 401) {
//         // read the return JSON and scan for a known 'redirect' action
//         try {
//           console.log(':: err = ' + JSON.stringify(err._body));
//           const smuiAuthViolation = JSON.parse(err._body) as SmuiAuthViolation;
//           console.log(':: smuiAuthViolation = ' + JSON.stringify(smuiAuthViolation));
//           if (smuiAuthViolation.action === 'redirect') {
//             const targetUrl = ((smuiAuthViolation.params.indexOf('{{CURRENT_SMUI_URL}}') !== -1)
//               ? smuiAuthViolation.params.replace('{{CURRENT_SMUI_URL}}', encodeURI(window.location.href))
//               : smuiAuthViolation.params);
//             // TODO solve with proper angular2 imports, and not by using plain window-object
//             window.location.href = targetUrl;
//           } else {
//             console.log(':: No known action found while evaluating SmuiAuthViolation JSON');
//           }
//         } catch (e) {
//           console.log(':: error while parsing SmuiAuthViolation JSON: ' + e);
//         }
//         // TODO passing an empty result object will result in JSON parsing exception of the calling (search management) service
//         return Observable.empty();
//       } else {
//         return Observable.throw(err);
//       }
//     });
//   }
// }
//
// /*
// bootstrap(MyApp, [
//   HTTP_PROVIDERS,
//     ROUTER_PROVIDERS,
//     provide(LocationStrategy, { useClass: HashLocationStrategy }),
//     provide(Http, {
//         useFactory:
//           (xhrBackend: XHRBackend, requestOptions: RequestOptions, router: Router) =>
//             new HttpAuthInterceptor(xhrBackend, requestOptions, router),
//         deps: [XHRBackend, RequestOptions, Router]
//     })
// ])
// .catch(err => console.error(err));
// */
