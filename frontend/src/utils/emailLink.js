// Links in our emails open the site as /?verify=CODE or /?reset=CODE.
// This reads that code once per page load, then removes it from the address bar
// so it isn't left in the browser history or shared by accident.

let taken = false
let link = null

export function takeEmailLink() {
  if (!taken) {
    taken = true
    const params = new URLSearchParams(window.location.search)
    if (params.get('verify')) link = { kind: 'verify', code: params.get('verify') }
    else if (params.get('reset')) link = { kind: 'reset', code: params.get('reset') }
    if (link) window.history.replaceState(null, '', window.location.pathname)
  }
  return link
}

/** Tests only: read the address bar again. */
export function forgetEmailLink() {
  taken = false
  link = null
}
