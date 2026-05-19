"use client";

import { useEffect, useState } from "react";
import { HealthStatus } from "@open-elements/ui";
import { useAppLayerTranslations } from "../../../translations/provider";

export function ServerStatusClient() {
  const t = useAppLayerTranslations();
  const [healthy, setHealthy] = useState<boolean | null>(null);

  useEffect(() => {
    fetch("/api/health")
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => setHealthy(data?.status === "UP"))
      .catch(() => setHealthy(false));
  }, []);

  return (
    <div>
      <h1 className="mb-6 font-heading text-2xl font-bold text-oe-dark">
        {t.nav.serverStatus}
      </h1>
      {healthy !== null && (
        <HealthStatus
          healthy={healthy}
          translations={{
            title: t.health.title,
            statusUp: t.health.statusUp,
            statusDown: t.health.statusDown,
          }}
        />
      )}
    </div>
  );
}
