# Design: Extract App Components into UI Package

## GitHub Issue

—

## Summary

Five app-level components (`language-switch`, `health-status`, `sidebar`, `tag-chips`, `tag-form`) are extracted from the CRM frontend into `@open-elements/ui`. Each component is made app-agnostic: no routing, no API calls, no direct auth imports. All data flows through props and callbacks. This enables reuse across the CRM, TODO app, and future applications.

This spec depends on Spec 087 (shadcn/ui + LanguageProvider infrastructure must be in the UI package first).

## Goals

- Extract 5 components into `@open-elements/ui` as app-agnostic, prop-driven components
- Design a flexible Sidebar with slot-based navigation and configurable sub-components
- Ensure all components work without any app-specific imports
- Add translations for component-internal strings to the UI package's i18n system
- Add tests for all extracted components

## Non-goals

- Extracting `session-provider`, `forbidden-page`, `api-key-list`, `webhook-list`, or `tag-list`
- Extracting auth logic (`auth.ts`, login page, logout route)
- Supporting more than DE/EN languages
- Adding new features to the components — this is extraction, not enhancement

## Technical Approach

### 1. LanguageSwitch

**Current state:** Imports `useLanguage` from `@/lib/i18n/language-context`, renders two buttons (DE/EN).

**After extraction:** Uses `useLanguage` from the UI package's own LanguageProvider (moved in Spec 087). No props needed — it reads language state from context.

**Location:** `packages/ui/src/components/language-switch.tsx`

```typescript
// No props needed — uses LanguageProvider context internally
export function LanguageSwitch(): JSX.Element
```

### 2. HealthStatus

**Current state:** Pure presentational component, receives `healthy: boolean` as prop.

**After extraction:** Identical API. Only change: translations for "Server Status", "Running", "Down" move into UI package i18n.

**Location:** `packages/ui/src/components/health-status.tsx`

```typescript
interface HealthStatusProps {
  readonly healthy: boolean;
}

export function HealthStatus({ healthy }: HealthStatusProps): JSX.Element
```

**Translations added to UI package:**
```typescript
health: {
  title: "Server Status",
  statusUp: "Running",
  statusDown: "Down",
}
```

### 3. TagChips

**Current state:** Receives `tagIds: string[]`, fetches each tag via `getTag(id)`, renders colored chips.

**After extraction:** Receives `tags: TagDto[]` directly (no fetching). The contrast color logic and hex validation stay in the component.

**Location:** `packages/ui/src/components/tag-chips.tsx`

```typescript
interface TagChipsProps {
  readonly tags: readonly TagDto[];
  readonly label?: string;
}

export function TagChips({ tags, label }: TagChipsProps): JSX.Element | null
```

**Rationale:** The `label` prop replaces the hardcoded `t.tags.label` translation. The consuming app passes the translated label. This keeps the component free of translation dependencies for domain-specific strings. If no label is provided, the label heading is not rendered.

### 4. TagForm

**Current state:** Full tag create/edit form with `createTag`/`updateTag` API calls, `router.push("/tags")` after save, and validation.

**After extraction:** API calls replaced by `onSave` callback. Navigation replaced by `onSuccess` and `onCancel` callbacks. Validation stays internal.

**Location:** `packages/ui/src/components/tag-form.tsx`

```typescript
interface TagFormProps {
  readonly tag?: TagDto;
  readonly onSave: (data: { name: string; description: string | null; color: string }) => Promise<void>;
  readonly onCancel: () => void;
  readonly translations: TagFormTranslations;
}

interface TagFormTranslations {
  readonly title: string;        // "Create Tag" or "Edit Tag"
  readonly name: string;
  readonly nameRequired: string;
  readonly namePlaceholder: string;
  readonly nameConflict: string;
  readonly description: string;
  readonly descriptionPlaceholder: string;
  readonly color: string;
  readonly colorRequired: string;
  readonly colorInvalid: string;
  readonly colorPlaceholder: string;
  readonly save: string;
  readonly cancel: string;
}

export function TagForm({ tag, onSave, onCancel, translations }: TagFormProps): JSX.Element
```

**Rationale:** Translations are passed as a prop object rather than using the UI package's i18n system because these are domain-specific labels ("Create Tag", "Edit Tag") that differ per app context. The `onSave` callback receives the validated form data; the component handles validation internally and shows errors. The `onSave` callback may throw — if it throws an `Error` with `message === "CONFLICT"`, the component shows the `nameConflict` error. For any other error, it shows a generic error message.

### 5. Sidebar

The most significant refactoring. The sidebar becomes a composable layout component.

**Location:** `packages/ui/src/components/sidebar.tsx` (may split into multiple files under `packages/ui/src/components/sidebar/`)

#### Component Architecture

```
Sidebar
├── SidebarHeader          (logo, app title, "Developed by" branding)
├── SidebarNav             (top slot — children)
├── SidebarNavBottom       (bottom slot — children, pushed to bottom via mt-auto)
│   └── CollapsibleGroup   (optional wrapper for grouped items)
├── LanguageSwitch         (built-in, between nav and user section)
└── UserSection            (optional — avatar, username, logout)
```

#### Sidebar (root)

```typescript
interface SidebarProps {
  readonly header?: React.ReactNode;     // custom header, defaults to SidebarHeader
  readonly appTitle: string;             // passed to default SidebarHeader
  readonly user?: UserSectionProps;      // if omitted, UserSection is not rendered
  readonly children: React.ReactNode;    // top nav slot
  readonly bottomChildren?: React.ReactNode; // bottom nav slot
  readonly menuLabel?: string;           // mobile menu aria label
}

export function Sidebar(props: SidebarProps): JSX.Element
```

**Behavior:**
- Desktop (md+): Fixed sidebar on the left, 256px wide (`w-64`)
- Mobile: Hidden sidebar, hamburger button in a top header bar, opens Sheet from left
- LanguageSwitch is always rendered between the navigation and user section
- Both desktop and mobile render the same content (header, nav slots, language switch, user section)

#### SidebarHeader

```typescript
interface SidebarHeaderProps {
  readonly appTitle: string;
  readonly logoIcon?: React.ReactNode;   // defaults to LayoutDashboard icon
  readonly homeHref?: string;            // defaults to "/"
}

export function SidebarHeader(props: SidebarHeaderProps): JSX.Element
```

Renders app title, optional logo icon, and the fixed "Developed by Open Elements" branding with the OE logo.

**Rationale:** The "Developed by Open Elements" section is fixed because all consuming apps are Open Elements products. The `homeHref` prop allows apps to configure where the header links to (CRM uses `/companies`, others may use `/`).

#### NavItem

```typescript
interface NavItemProps {
  readonly href: string;
  readonly icon: React.ReactNode;
  readonly label: string;
  readonly active?: boolean;
  readonly indented?: boolean;
  readonly onClick?: () => void;
}

export function NavItem(props: NavItemProps): JSX.Element
```

A single navigation link. The `active` prop controls the highlighted state. The consuming app determines which item is active (e.g., by comparing with the current pathname).

#### CollapsibleGroup

```typescript
interface CollapsibleGroupProps {
  readonly icon: React.ReactNode;
  readonly label: string;
  readonly defaultOpen?: boolean;
  readonly active?: boolean;           // any child is active
  readonly children: React.ReactNode;  // NavItem children
}

export function CollapsibleGroup(props: CollapsibleGroupProps): JSX.Element
```

A group of nav items that can be expanded/collapsed. Used for admin sections or any grouped navigation.

#### UserSection

```typescript
interface UserSectionProps {
  readonly userName: string;
  readonly avatarUrl?: string;
  readonly roles?: readonly string[];
  readonly onAvatarClick?: () => void;
  readonly onLogout: () => void;
  readonly translations: UserSectionTranslations;
}

interface UserSectionTranslations {
  readonly uploadAvatar: string;
  readonly logout: string;
  readonly noRoles: string;
}

export function UserSection(props: UserSectionProps): JSX.Element
```

**Behavior:**
- Shows avatar image (if `avatarUrl` provided) or fallback `CircleUser` icon
- Clicking avatar triggers `onAvatarClick` (app handles file upload flow)
- Username displayed with tooltip showing roles
- Logout button triggers `onLogout` callback

### App Integration Example (CRM)

```tsx
// In CRM app layout
import {
  Sidebar, NavItem, CollapsibleGroup, UserSection
} from "@open-elements/ui";

function AppSidebar() {
  const pathname = usePathname();
  const { data: session } = useSession();

  return (
    <Sidebar
      appTitle="Open CRM"
      user={{
        userName: session?.user?.name ?? "User",
        avatarUrl: hasAvatar ? getUserAvatarUrl() : undefined,
        onAvatarClick: () => fileInputRef.current?.click(),
        onLogout: () => { window.location.href = "/api/logout"; },
        translations: { uploadAvatar: t.sidebar.uploadAvatar, logout: t.user.logout, noRoles: t.user.noRoles },
        roles: session?.roles,
      }}
      bottomChildren={
        canSeeAdmin && (
          <CollapsibleGroup icon={<Settings />} label={t.nav.admin} active={isAdminRoute(pathname)}>
            <NavItem href="/admin/status" icon={<Activity />} label={t.nav.serverStatus} active={pathname.startsWith("/admin/status")} />
            {/* ... more admin items */}
          </CollapsibleGroup>
        )
      }
    >
      <NavItem href="/companies" icon={<Building2 />} label={t.nav.companies} active={pathname.startsWith("/companies")} />
      <NavItem href="/contacts" icon={<Users />} label={t.nav.contacts} active={pathname.startsWith("/contacts")} />
      <NavItem href="/tasks" icon={<CheckSquare />} label={t.nav.tasks} active={pathname.startsWith("/tasks")} />
      <NavItem href="/tags" icon={<Tag />} label={t.nav.tags} active={pathname.startsWith("/tags")} />
    </Sidebar>
  );
}
```

## Dependencies

- Spec 087 must be completed first (shadcn/ui components + LanguageProvider in UI package)
- Components use: `Sheet`, `Tooltip`, `Card`, `Label`, `Button`, `Input`, `Textarea` from UI package
- `lucide-react` for icons (already a peer dep)
- `next/link` — NavItem renders an `<a>` tag; the consuming app wraps with Next.js `Link` or passes `onClick`. **Alternative:** NavItem renders a plain `<a>` tag and the app can wrap it or use it directly. This avoids coupling the UI package to Next.js.

**Rationale for `<a>` over Next.js Link:** The UI package must not depend on Next.js. NavItem uses a plain `<a>` tag. For Next.js apps that need client-side navigation, the `onClick` prop can be used with `router.push`, or the app can wrap NavItem with Next.js `Link` using `asChild` pattern or a custom wrapper.

## Security Considerations

None — no behavioral changes, pure extraction.

## Open Questions

1. Should NavItem use a plain `<a>` tag, or accept a `renderLink` prop for framework-specific link components? Plain `<a>` is simpler; `renderLink` is more flexible.
