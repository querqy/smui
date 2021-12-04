import { Injectable} from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { EMPTY } from 'rxjs';

class SmuiAuthViolation {
  action: string; // supports 'redirect' currently
  params: string; // in case of 'redirect', param contains the redirect target (absolute URL)
}

@Injectable()
export class HttpAuthInterceptor implements HttpInterceptor {

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(catchError((err: any) => {
      if (err.status === 200 && err.name === "HttpErrorResponse" && err.url != null) {
        console.log('200 Response :: err = ' + JSON.stringify(err));
        //window.location.href = err.url

      }
      if (err.status === 401) {
        try {
          console.log('401 Response :: err = ' + JSON.stringify(err.error));
          const smuiAuthViolation = err.error as SmuiAuthViolation;
          console.log(':: smuiAuthViolation = ' + JSON.stringify(smuiAuthViolation));
          if (smuiAuthViolation.action === 'redirect') {
            // TODO solve with proper angular2 imports, and not by using plain window-object
            // this._router.navigate(['/login']);
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
      } else {
        return throwError(err);
      }
    }));
  }
}
