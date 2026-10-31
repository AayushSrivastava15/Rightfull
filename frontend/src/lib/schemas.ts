import { z } from "zod";

export const waitlistSchema = z.object({
  email: z.string().trim().min(1, "Please enter your email").email("That doesn't look like a valid email").max(255),
});
export type Waitlist = z.infer<typeof waitlistSchema>;
