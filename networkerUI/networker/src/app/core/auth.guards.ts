import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { map, take } from 'rxjs';

/** Sends signed-out visitors to the welcome page instead of straight to Auth0. */
export const requireLogin: CanActivateFn = () => {
  const router = inject(Router);
  return inject(AuthService).isAuthenticated$.pipe(
    take(1),
    map((ok) => ok || router.createUrlTree(['/welcome'])),
  );
};

export const requireLogout: CanActivateFn = () => {
  const router = inject(Router);
  return inject(AuthService).isAuthenticated$.pipe(
    take(1),
    map((ok) => !ok || router.createUrlTree(['/'])),
  );
};
