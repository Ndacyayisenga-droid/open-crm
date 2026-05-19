import { auth } from "@/auth";
import { createServerStatusPage } from "@open-elements/nextjs-app-layer";

export default createServerStatusPage({ auth });
