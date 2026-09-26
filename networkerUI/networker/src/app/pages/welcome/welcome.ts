import { Component, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';

@Component({
  selector: 'app-welcome',
  templateUrl: './welcome.html',
})
export class Welcome {
  private readonly auth = inject(AuthService);

  protected login() {
    this.auth.loginWithRedirect();
  }

  protected signup() {
    this.auth.loginWithRedirect({ authorizationParams: { screen_hint: 'signup' } });
  }
}
