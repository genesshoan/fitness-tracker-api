# Frontend Integration

Use the achievement endpoints for the badge collection, the unlock feed, and the
progress towards the next milestone.

| UI feature | Endpoint | Notes |
|---|---|---|
| Badge collection | `GET /api/v1/achievements` | One entry per global achievement, one per exercise for a scoped one |
| Badge detail | `GET /api/v1/achievements/{code}` | Returns a list, because a scoped achievement has one entry per exercise |
| Unlock feed | `GET /api/v1/achievements/feed?unseenOnly=true` | The badges the user has not seen yet, most recent first |
| Collection summary | `GET /api/v1/achievements/summary` | Totals, unseen count, and the closest milestones |
| Mark as seen | `POST /api/v1/achievements/seen` | Send the `id` of the feed entries once they are shown |

## Showing an unlock

The workout endpoints report the achievements that the event unlocked, in the
`achievements` field of the session and of the set. A set that beats a record
returns the badge it unlocked, so the "new record" feedback does not need a
second call.

A badge is described by its `code`, `name`, and `tier`. Its `iconKey` is the key
of the image that represents it, and it is `null` when the achievement has no
image yet.

## Progress

`progressRatio` goes from 0 to 1 and is the fraction of the threshold that is
already reached. It is `null` for the achievements that have no threshold, the
record-based ones, whose `currentValue` holds the current record instead.

`next` in the summary lists the milestones the user is closest to unlocking,
with the `remaining` amount already divided by the metric of each one. It is
already sorted, closest first.

## Marking as seen

The feed entries carry an `id`, which is the identifier of the state of the user,
not the `code` of the achievement. Send those identifiers to
`POST /api/v1/achievements/seen` once the user has seen them, and ask the feed
for `unseenOnly=true` afterwards.

## Scoped achievements

An achievement scoped to an exercise appears once per exercise the user earned
it on, and `exerciseId` and `exerciseName` tell which one is which. A global
achievement appears once, with both fields `null`.

A locked achievement scoped to an exercise is not in the catalog: there is no
exercise to show it against until the user earns it.

## Errors

Handle `401` by re-authenticating, `404` for an unknown code, and `400` for an
invalid body without retrying it unchanged.
