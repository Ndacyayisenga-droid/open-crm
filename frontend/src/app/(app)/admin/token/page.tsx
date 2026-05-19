import { auth } from "@/auth";
import { createBearerTokenPage } from "@open-elements/nextjs-app-layer";

export default createBearerTokenPage({ auth });
