"use client";

import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import { useSession } from "next-auth/react";
import { Activity, Bell, Building2, FileText, KeyRound, RefreshCw, Search, Settings, Tag, Users, Webhook } from "lucide-react";
import { Sidebar, NavItem, CollapsibleGroup, TooltipProvider } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { getCurrentUser } from "@/lib/api";
import { hasRole, ROLE_IT_ADMIN } from "@open-elements/nextjs-app-layer";

function isAdminRoute(pathname: string): boolean {
  return pathname.startsWith("/admin");
}

function CrmSidebar() {
  const t = useTranslations();
  const pathname = usePathname();
  const { data: session } = useSession();
  const canSeeAdmin = hasRole(session, ROLE_IT_ADMIN);
  const [avatarUrl, setAvatarUrl] = useState<string | undefined>(undefined);

  useEffect(() => {
    getCurrentUser()
      .then((user) => setAvatarUrl(user.avatarUrl ?? undefined))
      .catch(() => {});
  }, []);

  return (
      <Sidebar
        appTitle={t.app.title}
        homeHref="/companies"
        developedByText={t.app.developedBy}
        menuLabel={t.sidebar.menu}
        user={{
          userName: session?.user?.name ?? "User",
          avatarUrl: avatarUrl ?? session?.user?.image ?? undefined,
          roles: session?.roles,
          onLogout: () => { window.location.href = "/api/logout"; },
          translations: {
            uploadAvatar: t.sidebar.uploadAvatar,
            logout: t.user.logout,
            noRoles: t.user.noRoles,
          },
        }}
        bottomChildren={
          canSeeAdmin ? (
            <CollapsibleGroup
              icon={<Settings className="h-5 w-5" />}
              label={t.nav.admin}
              defaultOpen={isAdminRoute(pathname)}
              active={isAdminRoute(pathname)}
            >
              <NavItem href="/admin/status" icon={<Activity className="h-5 w-5" />} label={t.nav.serverStatus} active={pathname.startsWith("/admin/status")} indented />
              <NavItem href="/admin/token" icon={<KeyRound className="h-5 w-5" />} label={t.nav.bearerToken} active={pathname.startsWith("/admin/token")} indented />
              <NavItem href="/admin/brevo" icon={<RefreshCw className="h-5 w-5" />} label={t.nav.brevo} active={pathname.startsWith("/admin/brevo")} indented />
              <NavItem href="/admin/api-keys" icon={<KeyRound className="h-5 w-5" />} label={t.nav.apiKeys} active={pathname.startsWith("/admin/api-keys")} indented />
              <NavItem href="/admin/webhooks" icon={<Webhook className="h-5 w-5" />} label={t.nav.webhooks} active={pathname.startsWith("/admin/webhooks")} indented />
              <NavItem href="/admin/users" icon={<Users className="h-5 w-5" />} label={t.nav.users} active={pathname.startsWith("/admin/users")} indented />
              <NavItem href="/admin/audit-logs" icon={<FileText className="h-5 w-5" />} label={t.nav.auditLogs} active={pathname.startsWith("/admin/audit-logs")} indented />
            </CollapsibleGroup>
          ) : undefined
        }
      >
        <NavItem href="/updates" icon={<Bell className="h-5 w-5" />} label={t.nav.updates} active={pathname.startsWith("/updates")} />
        <NavItem href="/search" icon={<Search className="h-5 w-5" />} label={t.nav.search} active={pathname.startsWith("/search")} />
        <NavItem href="/companies" icon={<Building2 className="h-5 w-5" />} label={t.nav.companies} active={pathname.startsWith("/companies")} />
        <NavItem href="/contacts" icon={<Users className="h-5 w-5" />} label={t.nav.contacts} active={pathname.startsWith("/contacts")} />
        <NavItem href="/tags" icon={<Tag className="h-5 w-5" />} label={t.nav.tags} active={pathname.startsWith("/tags")} />
      </Sidebar>
  );
}

export default function AppLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <TooltipProvider>
      <CrmSidebar />
      <main className="md:ml-64 h-screen overflow-y-auto bg-oe-white">
        <div className="p-6 md:p-8">{children}</div>
      </main>
    </TooltipProvider>
  );
}
