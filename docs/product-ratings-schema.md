# SLGAN Product Ratings — Firestore Schema

Firestore is schemaless, so this feature uses a `productRatings` collection rather than a relational SQL table.

## Collection: `productRatings`

One document per signed-in customer per product. Document ID format:

`<productId>_<firebaseAuthUid>`

Example: `abc123_userUid456`

| Field | Type | Required | Description |
|---|---|---|---|
| `productId` | string | yes | Exact Firestore document ID from `products` |
| `productName` | string | yes | Product name saved with the rating |
| `userId` | string | yes | Firebase Authentication UID of the reviewer |
| `userName` | string | yes | Display name or email prefix |
| `rating` | integer | yes | Whole-star rating from 1 to 5 |
| `review` | string | yes | Optional review text, maximum 1,000 characters |
| `createdAt` | timestamp | yes | When the rating was first saved |
| `updatedAt` | timestamp | yes | When the rating was last edited |

## Aggregation shown in the app

The app listens to `productRatings` and groups records by `productId`. For each product it calculates:

- `averageRating`: arithmetic mean of all valid 1–5 ratings, displayed to one decimal place.
- `ratingCount`: number of valid rating documents.
- `myRating`: the current customer's rating, if one exists.

These are calculated from the rating documents, not stored as customer-editable fields on `products`. This avoids trusting client-written aggregate totals and does not require a Cloud Function.

## Security

The matching rule is in `firestore.rules`. Reads are public so product cards can display ratings. Create/update/delete require Firebase Authentication and are limited to the current user's own document. The rules validate the rating range, required identifiers, and review length.

## Publish the rules

Committing `firestore.rules` to GitHub does not automatically publish them to Firebase unless a deployment workflow is configured. In Firebase Console, open **Firestore Database → Rules**, copy the `productRatings` match block from this repository's `firestore.rules` into the current rules, and click **Publish**. Keep the existing product, customer, order, and address rules intact.
