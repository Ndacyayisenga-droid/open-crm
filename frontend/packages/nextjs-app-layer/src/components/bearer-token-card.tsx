"use client";

import { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { Copy, Check, Eye, EyeOff } from "lucide-react";
import { Button, Card, CardContent, CardHeader, CardTitle } from "@open-elements/ui";
import { useAppLayerTranslations } from "../translations/provider";

export function BearerTokenCard() {
  const t = useAppLayerTranslations();
  const S = t.admin.token;
  const { data: session } = useSession();
  const [visible, setVisible] = useState(false);
  const [copied, setCopied] = useState(false);
  const [remaining, setRemaining] = useState("");

  const token = session?.accessToken;
  const expiresAt = session?.expiresAt;

  useEffect(() => {
    if (!expiresAt) {
      setRemaining("");
      return;
    }
    const update = () => {
      const diff = expiresAt * 1000 - Date.now();
      if (diff <= 0) {
        setRemaining(S.expired);
      } else {
        const mins = Math.floor(diff / 60000);
        const secs = Math.floor((diff % 60000) / 1000);
        setRemaining(`${S.validFor} ${mins}:${String(secs).padStart(2, "0")} min`);
      }
    };
    update();
    const interval = setInterval(update, 10000);
    return () => clearInterval(interval);
  }, [expiresAt, S.validFor, S.expired]);

  const handleCopy = () => {
    if (!token) return;
    navigator.clipboard.writeText(token);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <Card className="border-oe-gray-light">
      <CardHeader>
        <CardTitle className="font-heading text-lg text-oe-dark">{S.title}</CardTitle>
      </CardHeader>
      <CardContent>
        {!token ? (
          <p className="text-sm text-oe-gray-mid">{S.noToken}</p>
        ) : (
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <code className="flex-1 rounded bg-oe-gray-lightest px-3 py-2 text-xs font-mono break-all">
                {visible ? token : "••••••••••••••••••••••••••••••••"}
              </code>
              <Button variant="outline" size="sm" onClick={() => setVisible(!visible)}>
                {visible ? <EyeOff className="mr-1 h-4 w-4" /> : <Eye className="mr-1 h-4 w-4" />}
                {visible ? S.hide : S.show}
              </Button>
              <Button variant="outline" size="sm" onClick={handleCopy}>
                {copied ? <Check className="mr-1 h-4 w-4 text-oe-green" /> : <Copy className="mr-1 h-4 w-4" />}
                {copied ? S.copied : S.copy}
              </Button>
            </div>
            {remaining && (
              <p className={`text-sm ${remaining === S.expired ? "text-oe-red" : "text-oe-gray-mid"}`}>
                {remaining}
              </p>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
