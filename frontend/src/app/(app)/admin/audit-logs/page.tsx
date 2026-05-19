import { auth } from "@/auth";
import { createAuditLogsPage } from "@open-elements/nextjs-app-layer";

export default createAuditLogsPage({ auth });
