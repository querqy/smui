export interface AuthViolation {
  action: string; // supports 'redirect' currently
  params: string; // in case of 'redirect', param contains the redirect target (absolute URL)
}
