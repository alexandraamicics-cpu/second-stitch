# Deploying Second Stitch

Vercel only hosts the **frontend** (React). It can't run a Spring Boot server, so the app goes live in three places, all on free plans:

| Part | Where | Why |
| --- | --- | --- |
| Database | **Neon** (PostgreSQL) | Free Postgres that doesn't need a server of its own |
| Backend | **Render** (Docker) | Runs the Java app from `backend/Dockerfile` |
| Frontend | **Vercel** | Builds and hosts the React app |

Do them in this order, because each step needs an address from the one before.

Before you start, make sure your latest code (including `backend/Dockerfile`) is pushed to GitHub, and that CI is green.

---

## 1. Database on Neon

1. Sign up at **neon.com** (you can log in with GitHub).
2. Create a project named `second-stitch` and pick the region closest to you (Singapore if offered).
3. On the project dashboard, click **Connect** and copy the connection string. It looks like this:

   ```
   postgresql://neondb_owner:AbC123xyz@ep-cool-name-123456.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
   ```

4. Split it into the three values Spring Boot needs:

   | Variable | Value from the example |
   | --- | --- |
   | `DATABASE_URL` | `jdbc:postgresql://ep-cool-name-123456.ap-southeast-1.aws.neon.tech/neondb?sslmode=require` |
   | `DATABASE_USERNAME` | `neondb_owner` |
   | `DATABASE_PASSWORD` | `AbC123xyz` |

   For the URL: change `postgresql://` to `jdbc:postgresql://` and **remove** the `user:password@` part.

Keep these somewhere private, like a notes app. **Never commit them to GitHub.**

---

## 2. Backend on Render

1. Sign up at **render.com** with GitHub.
2. Click **New → Web Service** and pick your `second-stitch` repo.
3. Fill in:
   - **Name:** `second-stitch-api` (this becomes part of the address)
   - **Root Directory:** `backend`
   - **Language / Runtime:** `Docker`
   - **Instance type:** `Free`
4. Under **Environment Variables**, add:

   | Key | Value |
   | --- | --- |
   | `SPRING_PROFILES_ACTIVE` | `postgres` |
   | `DATABASE_URL` | the JDBC URL from step 1 |
   | `DATABASE_USERNAME` | from step 1 |
   | `DATABASE_PASSWORD` | from step 1 |
   | `SEED_SAMPLE_DATA` | `true` (so visitors see sample pieces) |
   | `ADMIN_PASSWORD` | a long password only you know (needed to add, edit or delete on the live site) |
   | `CORS_ORIGINS` | leave this for now; you'll add it in step 4 |

5. Click **Create Web Service**. The first build takes several minutes because it downloads Maven and all the dependencies.
6. When the log says `Started SecondStitchApplication`, copy your service address from the top of the page, for example `https://second-stitch-api.onrender.com`.

✅ **Check:** open `https://second-stitch-api.onrender.com/api/items`. You should see JSON with the sample pieces.

---

## 3. Frontend on Vercel

1. Sign up at **vercel.com** with GitHub.
2. Click **Add New → Project** and import your `second-stitch` repo.
3. Set:
   - **Root Directory:** click **Edit** and choose `frontend`
   - **Framework Preset:** Vite (it should detect this automatically)
   - Leave the build command (`npm run build`) and output folder (`dist`) as they are
4. Open **Environment Variables** and add:

   | Key | Value |
   | --- | --- |
   | `VITE_API_URL` | your Render address, e.g. `https://second-stitch-api.onrender.com` (no `/` at the end) |

5. Click **Deploy**. When it finishes, copy your site's address, for example `https://second-stitch.vercel.app`.

---

## 4. Let the frontend talk to the backend (CORS)

Browsers block a website from calling an API on a different address unless the API allows it.

1. Back in Render, open your service and go to **Environment**.
2. Add `CORS_ORIGINS` = your Vercel address, e.g. `https://second-stitch.vercel.app` (no `/` at the end).
3. Save. Render restarts the backend automatically.

✅ **Check:** open your Vercel site. The closet loads with the sample pieces.

---

## 5. Finish up

- Add the live link to the top of your `README.md`, commit and push.
- From now on, every push to `main` redeploys automatically on both Vercel and Render.

---

## Things to know about free plans

- **The first visit can be slow.** Render's free plan puts the backend to sleep after 15 minutes without visitors, and waking it takes about a minute. Neon's free database also pauses when idle. If a recruiter will look at it, open the site yourself a minute beforehand to wake it up.
- **Photos are stored in Neon, not on Render.** Render's free plan wipes its disk whenever it sleeps or restarts, so the app saves photos in the database instead. The browser shrinks each photo to about 200 KB first, so Neon's free storage holds thousands.
- **Set an owner password.** In Render's **Environment**, add `ADMIN_PASSWORD` with a long password only you know. Visitors can then look around but not change anything; you click **Unlock editing** on the site and enter it. Without it, anyone with the link can edit. Adding sign-in is another good challenge.
- **`VITE_` variables are baked in when Vercel builds.** If you change `VITE_API_URL`, go to **Deployments** in Vercel and click **Redeploy**.

---

## Troubleshooting

| Problem | Fix |
| --- | --- |
| The Vercel site says **"Can't reach the server"** | Open the Render address in a new tab to wake the backend, wait a minute, then refresh. If it still fails, check `VITE_API_URL` in Vercel (no trailing `/`) and redeploy. |
| Browser console shows a **CORS** error | `CORS_ORIGINS` in Render must exactly match the Vercel address, including `https://` and without a trailing `/`. |
| Render log shows `Connection refused` or `password authentication failed` | Check the three `DATABASE_` values. The URL must start with `jdbc:postgresql://` and must not contain the username and password. |
| Render build fails during `mvn package` | Read the red lines in the build log, and make sure the project builds locally with `mvn package`. |
| Vercel build fails | Make sure **Root Directory** is set to `frontend`, and that `npm run build` works on your computer. |
