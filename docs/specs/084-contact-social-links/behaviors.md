# Behaviors: Contact Social Links

## Social Link Creation

### Create contact with GitHub link using username

- **Given** no contact exists
- **When** creating a contact with social link `{ networkType: "GITHUB", value: "hendrikebbers" }`
- **Then** the contact is created with one social link where `url` is `https://github.com/hendrikebbers` and `value` is `hendrikebbers`

### Create contact with GitHub link using @-prefixed username

- **Given** no contact exists
- **When** creating a contact with social link `{ networkType: "GITHUB", value: "@hendrikebbers" }`
- **Then** the contact is created with one social link where `url` is `https://github.com/hendrikebbers` and `value` is `hendrikebbers` (@ stripped)

### Create contact with GitHub link using full URL

- **Given** no contact exists
- **When** creating a contact with social link `{ networkType: "GITHUB", value: "https://github.com/hendrikebbers" }`
- **Then** the contact is created with one social link where `url` is `https://github.com/hendrikebbers` and `value` is `hendrikebbers` (extracted from URL)

### Create contact with multiple links for the same network

- **Given** no contact exists
- **When** creating a contact with two GitHub links `hendrikebbers` and `octocat`
- **Then** the contact is created with two social links, both with `networkType: "GITHUB"`

### Create contact with links across multiple networks

- **Given** no contact exists
- **When** creating a contact with a GitHub link, a LinkedIn link, and a Website link
- **Then** the contact is created with three social links, each with the correct network type and constructed URL

### Create contact with no social links

- **Given** no contact exists
- **When** creating a contact with an empty social links list
- **Then** the contact is created with zero social links

## Network-Specific URL Construction

### LinkedIn — username input

- **Given** social link input `{ networkType: "LINKEDIN", value: "hendrik-ebbers" }`
- **When** the link is validated and saved
- **Then** `url` is `https://linkedin.com/in/hendrik-ebbers`

### LinkedIn — /in/ prefix input

- **Given** social link input `{ networkType: "LINKEDIN", value: "/in/hendrik-ebbers" }`
- **When** the link is validated and saved
- **Then** `url` is `https://linkedin.com/in/hendrik-ebbers` and `value` is `hendrik-ebbers`

### X — username input

- **Given** social link input `{ networkType: "X", value: "hendrikEbworx" }`
- **When** the link is validated and saved
- **Then** `url` is `https://x.com/hendrikEbworx`

### X — @-prefixed username

- **Given** social link input `{ networkType: "X", value: "@hendrikEbworx" }`
- **When** the link is validated and saved
- **Then** `url` is `https://x.com/hendrikEbworx` and `value` is `hendrikEbworx`

### Mastodon — full handle input

- **Given** social link input `{ networkType: "MASTODON", value: "@hendrik@mastodon.social" }`
- **When** the link is validated and saved
- **Then** `url` is `https://mastodon.social/@hendrik`

### Mastodon — full URL input

- **Given** social link input `{ networkType: "MASTODON", value: "https://mastodon.social/@hendrik" }`
- **When** the link is validated and saved
- **Then** `url` is `https://mastodon.social/@hendrik` and `value` is `@hendrik@mastodon.social`

### BlueSky — standard handle

- **Given** social link input `{ networkType: "BLUESKY", value: "hendrik.bsky.social" }`
- **When** the link is validated and saved
- **Then** `url` is `https://bsky.app/profile/hendrik.bsky.social`

### BlueSky — custom domain handle

- **Given** social link input `{ networkType: "BLUESKY", value: "hendrik.openelements.com" }`
- **When** the link is validated and saved
- **Then** `url` is `https://bsky.app/profile/hendrik.openelements.com`

### Discord — numeric ID

- **Given** social link input `{ networkType: "DISCORD", value: "123456789012345678" }`
- **When** the link is validated and saved
- **Then** `url` is `https://discord.com/users/123456789012345678`

### YouTube — @handle input

- **Given** social link input `{ networkType: "YOUTUBE", value: "@hendrikebbers" }`
- **When** the link is validated and saved
- **Then** `url` is `https://youtube.com/@hendrikebbers`

### YouTube — handle without @ prefix

- **Given** social link input `{ networkType: "YOUTUBE", value: "hendrikebbers" }`
- **When** the link is validated and saved
- **Then** `url` is `https://youtube.com/@hendrikebbers`

### Website — full URL

- **Given** social link input `{ networkType: "WEBSITE", value: "https://open-elements.com" }`
- **When** the link is validated and saved
- **Then** `url` is `https://open-elements.com`

## Validation — Error Cases

### Website — input without protocol

- **Given** social link input `{ networkType: "WEBSITE", value: "example" }`
- **When** the link is validated
- **Then** the request is rejected with HTTP 400

### Discord — non-numeric input

- **Given** social link input `{ networkType: "DISCORD", value: "hendrik#1234" }`
- **When** the link is validated
- **Then** the request is rejected with HTTP 400

### Mastodon — handle without instance

- **Given** social link input `{ networkType: "MASTODON", value: "@hendrik" }`
- **When** the link is validated
- **Then** the request is rejected with HTTP 400

### GitHub URL provided for LinkedIn network type

- **Given** social link input `{ networkType: "LINKEDIN", value: "https://github.com/hendrik" }`
- **When** the link is validated
- **Then** the request is rejected with HTTP 400

### Empty value

- **Given** social link input `{ networkType: "GITHUB", value: "" }`
- **When** the link is validated
- **Then** the request is rejected with HTTP 400

### Unknown network type

- **Given** social link input `{ networkType: "INSTAGRAM", value: "hendrik" }`
- **When** the link is validated
- **Then** the request is rejected with HTTP 400

## Social Link Update

### Replace all social links on update

- **Given** a contact exists with two GitHub links
- **When** updating the contact with one LinkedIn link (no GitHub links)
- **Then** the contact has only the one LinkedIn link; the GitHub links are removed

### Update contact without touching social links (null)

- **Given** a contact exists with social links
- **When** updating the contact with `socialLinks: null`
- **Then** the existing social links remain unchanged

### Clear all social links

- **Given** a contact exists with social links
- **When** updating the contact with `socialLinks: []` (empty list)
- **Then** the contact has zero social links

## Social Link Deletion (Cascade)

### Delete contact removes social links

- **Given** a contact exists with three social links
- **When** the contact is deleted
- **Then** all associated social links are also deleted (CASCADE)

## Migration

### Existing LinkedIn URL is migrated

- **Given** a contact has `linkedin_url = "https://linkedin.com/in/hendrik-ebbers"` in the old schema
- **When** migration V23 runs
- **Then** a row exists in `contact_social_links` with `network_type = 'LINKEDIN'`, `value = 'https://linkedin.com/in/hendrik-ebbers'`, `url = 'https://linkedin.com/in/hendrik-ebbers'`
- **And** the `linkedin_url` column no longer exists on `contacts`

### Contact without LinkedIn is not affected

- **Given** a contact has `linkedin_url = NULL` in the old schema
- **When** migration V23 runs
- **Then** no social link row is created for that contact

## Search

### Find contact by GitHub username

- **Given** a contact exists with a GitHub social link `value = "hendrikebbers"`
- **When** searching contacts with `search = "hendrikebbers"`
- **Then** the contact appears in the results

### Find contact by partial social link value

- **Given** a contact exists with a GitHub social link `value = "hendrikebbers"`
- **When** searching contacts with `search = "hendrik"`
- **Then** the contact appears in the results

## Brevo Integration

### Brevo import creates LinkedIn social link

- **Given** a Brevo contact has a LinkedIn URL
- **When** the Brevo import runs
- **Then** the contact is created with a LINKEDIN social link

### Brevo reimport updates LinkedIn social link

- **Given** a contact imported from Brevo has a LINKEDIN social link
- **When** Brevo reimport runs with an updated LinkedIn URL
- **Then** the existing LINKEDIN social link is replaced with the new one

### Brevo contact social links are readonly

- **Given** a contact was imported from Brevo (has `brevoId`)
- **When** trying to update the contact's social links
- **Then** the request is rejected with HTTP 400

## CSV Export

### Social links exported as comma-separated URLs

- **Given** a contact has a GitHub link (`https://github.com/hendrik`) and a LinkedIn link (`https://linkedin.com/in/hendrik`)
- **When** exporting contacts to CSV with the SocialLinks column selected
- **Then** the SocialLinks cell contains `https://github.com/hendrik, https://linkedin.com/in/hendrik`

### Contact with no social links has empty CSV cell

- **Given** a contact has no social links
- **When** exporting contacts to CSV with the SocialLinks column selected
- **Then** the SocialLinks cell is empty

## Detail View Display

### Social links grouped by network with icons

- **Given** a contact has 2 GitHub links and 1 LinkedIn link
- **When** viewing the contact detail page
- **Then** LinkedIn links appear first (with LinkedIn icon on first row), then GitHub links (with GitHub icon on first row)
- **And** all link values are left-aligned at the same horizontal position

### Networks without links are hidden

- **Given** a contact has only a GitHub link
- **When** viewing the contact detail page
- **Then** only the GitHub section is shown; no empty rows for other networks

### Social links are clickable and copyable

- **Given** a contact has a GitHub social link
- **When** viewing the contact detail page
- **Then** the link text opens the full URL when clicked
- **And** a copy button allows copying the URL to clipboard

## Edit Form

### Add new social link via + button

- **Given** the contact edit form is open
- **When** clicking the "+" button
- **Then** a new row appears with a network dropdown and text input

### Delete social link via X button

- **Given** the contact edit form shows a social link row
- **When** clicking the X/delete button on that row
- **Then** the row is removed from the form

### Network dropdown shows all 8 options

- **Given** the contact edit form has a social link row
- **When** opening the network dropdown
- **Then** all 8 networks are listed: GitHub, LinkedIn, X, Mastodon, BlueSky, Discord, YouTube, Website

### Placeholder adapts to selected network

- **Given** a social link row with GitHub selected
- **When** changing the network to Mastodon
- **Then** the placeholder text changes to reflect the expected Mastodon input format

## Print View

### Social links not shown in print

- **Given** a contact has social links
- **When** opening the print view
- **Then** social links are not displayed in the printed table
