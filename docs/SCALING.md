# Speed and scale

Written after the owner asked whether 5,000 or 10,000 users would slow the app
down. Short answer: **the app does not get slower with more users.** The server
does. This note records what the app does about it, what it deliberately does
not do, and what has to happen on the WordPress side.

## The shape of the system

```
Android app  ──HTTP──▶  WordPress + mrdastbaaf-bridge  ──▶  MySQL / WooCommerce
   10k users                  one server
```

Every user's request lands on the same PHP process. The app's job is therefore
**not** to be clever — it is to ask for as little as possible, as rarely as
possible, and to keep working when the answer does not arrive.

A client that is fast on a slow server is a client that asks once.

## What the app does

| Behaviour | Why it matters at 10k users |
|---|---|
| `/app-ui` is fetched once per launch and shared | The shell, the home tab and the account card all want it. Three callers, one request — the repository holds a `Mutex` so the second caller waits instead of opening a second connection. |
| 60-second memory cache on `/app-ui` | Re-entering the app from the background costs nothing. |
| 60-second **disk** cache on `/app-ui`, `/shop/products`, `/memberships/plans` | A warm launch makes zero requests for these three. Also makes the app open with content in a dead zone. |
| No polling anywhere | The app never asks "anything new?" on a timer. Refresh is a user action. |
| Images cached by Coil (memory + disk) | Product photos are the largest bytes in the app and they are immutable per URL. |
| Catalogue loaded once per session | Tab switches do not refetch; pull-to-refresh does. |
| One request per screen | No screen makes a second call to fill a detail the first call could have carried. |

The disk cache needed an interceptor. The bridge sends `Cache-Control: no-store`
on every response — correct for authenticated data, expensive for public data.
`PublicCacheInterceptor` rewrites that header to `public, max-age=60` for the
three anonymous endpoints only. Authenticated responses are never cached on disk.

**Trade-off, stated plainly:** an edit in the panel can take up to a minute to
appear. That is why the window is a minute.

## What breaks first, in order

1. **Every request boots WordPress.** Even with LiteSpeed, `no-store` means PHP
   runs, plugins load, and MySQL answers. This is the whole cost, and it is paid
   per request, not per user.
2. **`GET /shop/products` instantiates every product.** The handler calls
   `wc_get_product()` inside a loop over up to 200 products. One request, two
   hundred product objects. This is the most expensive public endpoint by far.
3. **Authenticated endpoints cannot be cached.** `/me`, `/me/orders`,
   `/memberships/status` hit MySQL per user. At 10k daily users with 3 opens a
   day that is ~30k small queries a day — survivable, but it is what a burst
   turns into a queue.
4. **Nothing is paginated.** Fine for 40 courses; a problem at 400.

## What to do on the server, in order of impact

1. **Stop sending `no-store` on public endpoints.** For `/app-ui`,
   `/app-pages`, `/shop/products`, `/memberships/plans` and the course list,
   send `Cache-Control: public, max-age=60, stale-while-revalidate=300` plus an
   `ETag`. LiteSpeed then serves them without starting PHP at all — this single
   change is worth more than everything the app does.
2. **Fix the product loop.** Replace the per-product `wc_get_product()` with a
   single `wc_get_products()` call (or prime the object cache), and add
   `'fields' => 'ids'` where only ids are needed.
3. **Object cache.** Redis or Memcached in front of MySQL. WordPress supports it
   with a drop-in; the plugin's own transients then stop being table reads.
4. **Paginate the catalogue** (`?page=&per_page=`) before it passes a few hundred
   rows, and let the app ask for the next page as the user scrolls.
5. **Long-cache the images** at the CDN (`max-age=31536000, immutable`). They are
   content-addressed by filename in practice and never change.

None of these require an app release except (4).

## What the app will not do

- **No background sync.** A timer that fires for every install is the fastest way
  to turn 10k users into 10k requests per minute.
- **No websockets.** Nothing in this product changes second to second.
- **No client-side cache longer than a minute** for owner-edited content. Stale
  prices are worse than a slow screen.

## Measuring

The honest way to know which of the above matters is a server-side counter, not a
guess: log the path and the PHP time for a day, then look at the top ten. Until
that exists, the order above is a reasonable prior — not a measurement.
