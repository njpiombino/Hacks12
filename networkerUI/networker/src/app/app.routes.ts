import { Routes } from '@angular/router';

import { requireLogin, requireLogout } from './core/auth.guards';
import { Shell } from './shell';

export const routes: Routes = [
  {
    path: 'welcome',
    canActivate: [requireLogout],
    loadComponent: () => import('./pages/welcome/welcome').then((m) => m.Welcome),
  },
  {
    path: '',
    component: Shell,
    canActivate: [requireLogin],
    children: [
      { path: '', title: 'Home · Networker', loadComponent: () => import('./pages/home/home').then((m) => m.Home) },
      { path: 'people', title: 'People · Networker', loadComponent: () => import('./pages/people/people').then((m) => m.People) },
      { path: 'people/:id', title: 'Person · Networker', loadComponent: () => import('./pages/person/person').then((m) => m.PersonPage) },
      { path: 'calendar', title: 'Calendar · Networker', loadComponent: () => import('./pages/calendar/calendar').then((m) => m.Calendar) },
      { path: 'chat', title: 'Assistant · Networker', loadComponent: () => import('./pages/chat/chat').then((m) => m.Chat) },
      { path: 'profile', title: 'Your profile · Networker', loadComponent: () => import('./pages/profile/profile').then((m) => m.Profile) },
    ],
  },
  { path: '**', redirectTo: '' },
];
