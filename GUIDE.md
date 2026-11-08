# Second Stitch: step-by-step guide

This guide takes you from the zip file to a running app on GitHub with green tests, and then helps you make the project truly yours. Work through it in order. Each step ends with a checkpoint so you know it worked.

> **Important:** recruiters and interviewers will ask you about this code. Don't just push it and move on. Steps 9 and 10 are where it becomes *your* project: read the code tour, then build at least two of the challenges yourself.

---

## What's in the folder

```
second-stitch/
├── backend/                 Java + Spring Boot API
│   ├── pom.xml              dependencies (like package.json for Java)
│   └── src/
│       ├── main/java/com/secondstitch/
│       │   ├── item/        closet pieces: entity, controller, service, photos
│       │   ├── outfit/      saved outfits
│       │   ├── stats/       the savings numbers
│       │   ├── common/      money maths + error handling
│       │   └── config/      sample data, CORS
│       └── test/java/...    JUnit tests
├── frontend/                React + Vite app
│   └── src/
│       ├── api.js           every call to the backend
│       ├── App.jsx          header + tabs
│       ├── pages/           Closet, MixMatch, Stats
│       ├── components/      HangTag, ItemForm, Modal, ItemPhoto
│       ├── utils/           format + mix-and-match logic (with tests)
│       └── styles.css
├── .github/workflows/ci.yml GitHub Actions: runs all tests on every push
├── docs/                    screenshots for the README
├── README.md                what recruiters see first
└── GUIDE.md                 this file
```

---

## Step 1: Install the tools

Install these once. Restart your terminal after installing so it picks them up.

| Tool | Get it from | Check it worked |
| --- | --- | --- |
| Java 21 (JDK) | adoptium.net, choose **Temurin 21 LTS** | `java -version` shows 21 |
| Node.js | nodejs.org, choose the **LTS** version | `node -v` shows 20.19+ or 22+ |
| Git | git-scm.com | `git --version` |
| IntelliJ IDEA Community | jetbrains.com/idea (scroll to Community, it's free) | opens |
| VS Code | code.visualstudio.com | opens |

**Maven (optional):** IntelliJ already includes Maven, so you can skip this. If you'd rather run things from the terminal, install it:
- Windows: `winget install Apache.Maven` (or download from maven.apache.org and add its `bin` folder to PATH)
- Mac: `brew install maven`

Check with `mvn -v`.

**Tip:** as a student you can get IntelliJ IDEA Ultimate free through the JetBrains student program, using your school email.

✅ **Checkpoint:** `java -version`, `node -v` and `git --version` all print a version.

---

## Step 2: Put the project on GitHub

1. Unzip `second-stitch.zip` somewhere easy, such as `Documents/projects/second-stitch`.
2. On github.com, click **New repository**.
   - Name: `second-stitch`
   - Public (so recruiters can see it)
   - **Don't** tick "Add a README" (you already have one)
3. Open a terminal in the `second-stitch` folder and run:

```bash
git init
git add .
git commit -m "Initial commit: Spring Boot API and React frontend"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/second-stitch.git
git push -u origin main
```

4. In `README.md`, replace `YOUR-USERNAME` in the badge line with your GitHub username. Commit and push:

```bash
git add README.md
git commit -m "Point CI badge at my repo"
git push
```

✅ **Checkpoint:** your repo page on GitHub shows the README with the screenshots.

---

## Step 3: Run the backend

### Option A: IntelliJ (easiest)

1. **File → Open**, and choose the `backend` folder (the one containing `pom.xml`).
2. If it asks, click **Trust Project**. Wait while it downloads dependencies. The progress bar is at the bottom right, and the first time takes a few minutes.
3. If it asks for an SDK: **File → Project Structure → SDK → Add SDK → JDK**, then choose your Java 21 folder.
4. Open `src/main/java/com/secondstitch/SecondStitchApplication.java` and click the green ▶ next to `main`.

### Option B: Terminal

```bash
cd backend
mvn spring-boot:run
```

When it's ready, the last lines say something like `Started SecondStitchApplication in 3.2 seconds` and `Added 8 sample items and 2 sample outfits`.

Now open **http://localhost:8080/api/items** in your browser. You should see JSON listing the sample pieces.

**Bonus:** open **http://localhost:8080/h2-console** to browse the actual database tables.
- JDBC URL: `jdbc:h2:file:./data/closet`
- User name: `sa`
- Password: leave empty

Try running `SELECT * FROM ITEM;` there.

✅ **Checkpoint:** `/api/items` shows JSON with "Straight-leg 501s" in it.

---

## Step 4: Run the frontend

Keep the backend running. Open a **second** terminal, for example in VS Code with **File → Open Folder → frontend**, then **Terminal → New Terminal**:

```bash
npm install
npm run dev
```

Open the address it prints (usually **http://localhost:5173**).

After your first `npm install`, a `package-lock.json` file appears. Commit it, because it records the exact versions you installed:

```bash
git add frontend/package-lock.json
git commit -m "Add frontend lockfile"
git push
```

✅ **Checkpoint:** you see the closet with price tags hanging off each piece.

---

## Step 5: Try every feature

Do these once so you know the app end to end. You'll be demoing it in interviews.

1. **Closet:** click **Add a piece**. Fill it in, upload a photo, and save.
2. Click **+** next to *Times worn* on a piece and watch the cost per wear drop. Click **−** to undo it. The − button is greyed out at 0, so the count can't go negative.
3. Click the category chips to filter.
4. **Edit** a piece and change its price.
5. **Mix & match:** click pieces on the rack to add them to your look, and watch the receipt on the right add up the price. Pick a second top and see it swap out the first; pick a dress and the top and bottom come off. Take pieces off with **×** or by clicking them again. Pick just a top, then try **Fill the gaps for me**. Name the look and **Save outfit**.
6. Under **Saved outfits**, try **Mix again**, then click **+** under *Times worn* on an outfit and go back to the Closet. Every piece in it went up by one wear. Click **−** on the outfit to undo it.
7. **Savings:** check the receipt.
8. Try to add a piece with no name. You'll see the validation message that comes from the Java backend.

✅ **Checkpoint:** everything above works. If something doesn't, see Troubleshooting at the end.

---

## Step 6: Run the tests

### Backend (JUnit)

- IntelliJ: right-click the `src/test/java` folder and choose **Run 'All Tests'**.
- Terminal: `cd backend` then `mvn test`

There are two kinds of tests:
- `MoneyTest`: **unit tests.** Pure logic with no Spring and no database, so they run in milliseconds.
- `ItemApiTest` and `OutfitAndStatsApiTest`: **integration tests.** They start the real app with an empty in-memory database and call the API the same way the frontend does.

### Frontend (Vitest)

```bash
cd frontend
npm test
```

Use `npm run test:watch` to rerun the tests automatically every time you save a file.

### Watch a test fail on purpose

Do this once. It's the core habit of test-driven development (TDD).

1. In `backend/src/main/java/com/secondstitch/common/Money.java`, change `Math.max(wearCount, 1)` to `Math.max(wearCount, 2)`.
2. Run `MoneyTest`. You'll see red, and the message tells you exactly what's wrong.
3. Change it back and watch it go green.

✅ **Checkpoint:** all backend and frontend tests pass.

---

## Step 7: Watch CI run on GitHub

You already pushed, so open your repo and click the **Actions** tab. You'll see the **CI** workflow running two jobs, Backend and Frontend. In a few minutes both should get a green check, and the badge on your README turns green.

From now on, every push runs every test automatically. This is the "continuous integration" the job posting mentions.

✅ **Checkpoint:** a green check next to your latest commit.

---

## Step 8: Work like a team (agile and Git habits)

The posting asks for agile, DevOps and collaboration. Show it in how you work, not just in the code.

**1. Make a project board.** On GitHub go to **Projects → New project → Board**, with columns *To do / In progress / Done*.

**2. Turn each piece of work into an Issue**, for example:
- "Add a favourite flag to pieces"
- "Sort the closet by cost per wear"
- "Show sensory notes on each tag"

**3. Work on a branch for each issue, and merge with a pull request:**

```bash
git checkout -b feature/favourites
# ...make your changes, run the tests...
git add .
git commit -m "Add favourite flag to items (closes #3)"
git push -u origin feature/favourites
```

Then on GitHub click **Compare & pull request**. Wait for CI to go green, then **Merge**. Writing `closes #3` closes the issue automatically.

**4. Plan in one-week "sprints".** Pick 2–3 issues each week. At the end, write a few lines in the issue about what went well.

A history of small, clear PRs with green CI checks tells a recruiter more than one giant "final version" commit.

---

## Step 9: Understand the code

Follow one click all the way through. When you click **+** to add a wear:

1. **`WearStepper.jsx`**: the + button calls `onAdd`, which `HangTag.jsx` connects to `onWear(item)`.
2. **`pages/Closet.jsx`**: `handleWear` calls `wearItem(item.id)` from the API file.
3. **`api.js`**: `wearItem` sends `POST /api/items/7/wear`.
4. **`vite.config.js`**: the proxy forwards `/api/...` to `http://localhost:8080`.
5. **`ItemController.java`**: `@PostMapping("/{id}/wear")` receives it and calls the service.
6. **`ItemService.java`**: `recordWear` loads the item, calls `item.recordWear()`, and the `@Transactional` annotation saves the change to the database.
7. **`ItemResponse.java`**: turns the entity into JSON and calculates `costPerWear` using **`Money.java`**.
8. Back in **`Closet.jsx`**: `replace(updated)` swaps the new item into the list, React re-renders the tag, and `notify(...)` shows the message at the bottom.

Questions to make sure you understand it. Try answering them out loud, as you would in an interview:

- Why does the app have `ItemRequest` and `ItemResponse` instead of sending the `Item` entity directly?
- What does `@Valid` do, and where do the error messages come from?
- Why does deleting an item first remove it from outfits? (Hint: look at the `outfit_items` table in the H2 console.)
- Why are the frontend tests able to run without the backend? (Look at `vi.mock` in `Closet.test.jsx`.)
- What stops someone from requesting `../../secret-file` through the photo endpoint? (Look at `resolveSafely`.)
- The − button sends `DELETE /api/items/7/wear`. Why does `Item.removeWear()` check for zero on the server, even though the button is already disabled at 0 in the browser?
- On the Mix & match page, how does `addToLook()` in `utils/mixer.js` decide which pieces to take off? Why is that logic kept outside the React component?

If any of these feel fuzzy, ask me about them and I'll explain.

---

## Step 10: Make it yours (challenges)

Do at least two of these yourself. Each one touches the backend, frontend and tests, which is exactly the full-stack work the role describes. Make each one an Issue, a branch and a PR, as in Step 8.

**1. Favourite pieces (easy)**
- Add `private boolean favourite;` to `Item.java`, plus a getter and setter.
- Add it to `ItemRequest`, `applyTo` and `ItemResponse`.
- Add a ♥ toggle button to `HangTag.jsx`.
- Write a test in `ItemApiTest` that creates a favourite and checks the JSON.

**2. Sort the closet (easy)**
- Add a "Sort by" dropdown in `Closet.jsx`: newest, most worn, cheapest per wear.
- You can sort on the frontend with `[...items].sort(...)`.
- Write a Vitest test for the sort function.

**3. Sensory notes (medium; ties to your neurodiversity interest)**
- Add fields such as `texture` (soft / neutral / scratchy) and `tagFree` (yes/no).
- Show them on the tag and add a "Comfy only" filter.
- In an interview, explain why you added it. That's a great story.

**4. Search (medium)**
- Add `findByNameContainingIgnoreCase` to `ItemRepository`, and a `?q=` parameter to the controller.
- Add a search box in `Closet.jsx`.
- Write an API test.

**5. Auto-colour from the photo (stretch; "innovative")**
- In the browser, draw the uploaded image onto a `<canvas>`, average the pixel colours, and fill in the swatch automatically.

**6. Deploy it (stretch; DevOps)**
- Follow **DEPLOY.md**: database on Neon, backend on Render, frontend on Vercel.
- Put the live link at the top of your README.

---

## Step 11: Make your photos look good

The app shows your photos in a 4:5 frame, so consistent photos make the closet look curated:

- Use the **same plain background** every time: a white sheet, a wooden floor, or beige linen.
- Use **natural window light** and no flash.
- Shoot **straight down**, with the piece laid flat or neatly folded.
- **Crop to 4:5 or square** so nothing important gets cut off.
- Pieces without photos show a colour swatch with an outline, so set the **Swatch** colour to match the piece.

---

## Step 12: Put it on your resume

Example bullets you can adapt. Only claim what you actually did.

> **Second Stitch**, a full-stack wardrobe tracker | Java, Spring Boot, React, GitHub Actions
> - Built a REST API with Spring Boot and JPA for items, outfits, photo uploads and savings analytics, with validation and consistent JSON error handling
> - Wrote 45+ automated tests (JUnit and MockMvc integration tests, Vitest and React Testing Library) running in a GitHub Actions CI pipeline on every push
> - Designed an accessible React UI (keyboard and screen-reader support, reduced motion, dark mode) including a pick-and-add outfit builder with live pricing and a "fill the gaps" helper to reduce decision fatigue

For interviews, prepare a 60-second story: what problem it solves for you, one technical decision you made and why, one bug you hit and how you fixed it, and what you'd build next.

---

## Troubleshooting

| Problem | Fix |
| --- | --- |
| `Port 8080 was already in use` | Another copy is running. Stop it (the red ■ in IntelliJ, or Ctrl+C in the terminal) and start again. |
| Frontend says **"Can't reach the server"** | The backend isn't running. Start it (Step 3) and refresh the page. |
| IntelliJ shows red errors everywhere | Maven hasn't finished importing. Open the **Maven** tool window on the right and click ⟳ (Reload). Also check the SDK is set to 21. |
| `java: invalid target release: 21` | IntelliJ is using an older JDK. Set the project SDK to 21 (Step 3). |
| `npm install` errors about Node version | Update Node to the current LTS. |
| `mvn` is "not recognized" | Maven isn't installed or isn't on PATH. Use IntelliJ (Option A) instead. |
| I want a fresh start with the sample data | Stop the backend, delete `backend/data/` and `backend/uploads/`, and start it again. |
| CI fails on GitHub but passes locally | Click into the failed job in the **Actions** tab and read the red step. It's usually a test that depends on something on your computer. |

If you get stuck, copy the **whole** error message and send it to me.
