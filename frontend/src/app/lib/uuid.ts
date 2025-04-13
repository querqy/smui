
// taken from https://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript
export function randomUUID(): string {
  /* eslint-disable */
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
  /* eslint-enable */
}
