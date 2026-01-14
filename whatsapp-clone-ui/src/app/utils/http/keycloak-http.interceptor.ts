import {HttpHeaders, HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {KeycloakService} from '../keycloak/keycloak.service';
import { from, switchMap } from 'rxjs';

export const keycloakHttpInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloakService = inject(KeycloakService);
  // const token = keycloakService.keycloak.token;
  return from(keycloakService.getToken()).pipe(
    switchMap((token) => {
      // Clone request và gắn token mới nhất vào header
      const authReq = req.clone({
        headers: new HttpHeaders({
          Authorization: `Bearer ${token}`
        })
      });
      return next(authReq);
    })
  );
};
