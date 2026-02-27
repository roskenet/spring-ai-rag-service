import { controller } from '../config/oauth2';

export const getToken = async () => {
  if (typeof window === 'undefined' || !controller) {
    throw new Error('OAuth controller not available on server side');
  }
  return controller.signIn();
};

export const isAuthenticated = () => {
  if (typeof window === 'undefined' || !controller) {
    return false;
  }
  return controller.isAuthenticated();
};

export const signOut = async () => {
  if (typeof window === 'undefined' || !controller) {
    throw new Error('OAuth controller not available on server side');
  }
  return controller.signOut();
};