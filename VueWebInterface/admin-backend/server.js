const express = require("express");
const sqlite3 = require("sqlite3").verbose();
const cors = require("cors");

const app = express();
app.use(express.json({ limit: "50mb" }));
app.use(express.urlencoded({ extended: true, limit: "50mb" }));
app.use(cors());

const db = new sqlite3.Database("./education.db", (err) => {
    if (err) {
        console.error('Chyba pri pripojení k databáze', err);
    } else {
        db.run("PRAGMA foreign_keys = ON;");
    }
});

db.serialize(() => {
    db.run(`CREATE TABLE IF NOT EXISTS courses (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        icon TEXT,
        area_color TEXT,
        visible BOOLEAN DEFAULT TRUE,
        description TEXT
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS chapters (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        course_id INTEGER,
        position INTEGER DEFAULT 0,
        visible BOOLEAN DEFAULT TRUE, -- Pridaný stĺpec pre viditeľnosť
        FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS lessons (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        chapter_id INTEGER,
        position INTEGER DEFAULT 0,
        visible BOOLEAN DEFAULT TRUE, -- Pridaný stĺpec pre viditeľnosť
        FOREIGN KEY(chapter_id) REFERENCES chapters(id) ON DELETE CASCADE
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS tasks (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        lesson_id INTEGER,
        type TEXT CHECK(type IN ('translation', 'matching_definitions', 'matching_images', 'categorization', 'context_choice', 'sentence_building', 'gap_filling')),
        data TEXT NOT NULL,
        position INTEGER DEFAULT 0,
        points REAL DEFAULT 0.0,
        visible BOOLEAN DEFAULT TRUE, -- Pridaný stĺpec pre viditeľnosť
        FOREIGN KEY(lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS players (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER,
        name TEXT NOT NULL,
        email TEXT UNIQUE NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS player_scores (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        player_id INTEGER,
        task_id INTEGER,
        score REAL DEFAULT 0.0,
        attempt_count INTEGER DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE,
        FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE
    
    )`);

});

app.get("/courses", (req, res) => {
    db.all("SELECT * FROM courses", (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

app.post("/courses", (req, res) => {
    const { name, area_color, icon, description } = req.body;
    db.run("INSERT INTO courses (name, area_color, icon, description) VALUES (?, ?, ?, ?)",
        [name, area_color, icon, description],
        function (err) {
            if (err) return res.status(500).json({ error: err.message });
            res.json({ id: this.lastID });
        }
    );
});


app.put("/courses/:id", (req, res) => {
    const { name, area_color, icon, visible, description } = req.body;
    db.run("UPDATE courses SET name = ?, area_color = ?, icon = ?, visible = ?, description = ? WHERE id = ?",
        [name, area_color, icon, visible, description, req.params.id],
        (err) => {
            if (err) return res.status(500).json({ error: err.message });
            res.json({ updated: true });
        }
    );
});


app.delete("/courses/:id", (req, res) => {
    db.run("DELETE FROM courses WHERE id = ?", req.params.id, (err) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ deleted: true });
    });
});

app.get("/courses/:course_id/chapters", (req, res) => {
    db.all("SELECT * FROM chapters WHERE course_id = ?", [req.params.course_id], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

app.post("/courses/:course_id/chapters", (req, res) => {
    const { name } = req.body;

    db.get("SELECT COUNT(*) as count FROM chapters WHERE course_id = ?", [req.params.course_id], (err, row) => {
        if (err) return res.status(500).json({ error: err.message });

        const newPosition = row.count;

        db.run("INSERT INTO chapters (name, course_id, position) VALUES (?, ?, ?)",
            [name, req.params.course_id, newPosition],
            function (err) {
                if (err) return res.status(500).json({ error: err.message });
                res.json({ id: this.lastID, position: newPosition });
            }
        );
    });
});


app.put("/courses/:course_id/chapters/reorder", (req, res) => {
    const { orderedChapters } = req.body;

    const queries = orderedChapters.map((chapter, index) => {
        return new Promise((resolve, reject) => {
            db.run("UPDATE chapters SET position = ? WHERE id = ?", [index, chapter.id], (err) => {
                if (err) reject(err);
                else resolve();
            });
        });
    });

    Promise.all(queries)
        .then(() => res.json({ updated: true }))
        .catch((err) => res.status(500).json({ error: err.message }));
});


app.put("/chapters/:id", (req, res) => {
    const { name, visible } = req.body;
    console.log("Server prijal požiadavku na aktualizáciu kapitoly ID:", req.params.id, "s dátami:", req.body);
    db.run("UPDATE chapters SET name = ?, visible = ? WHERE id = ?", [name, visible, req.params.id], (err) => {
        if (err) {
            console.error("Chyba pri aktualizácii kapitoly v databáze:", err);
            return res.status(500).json({ error: err.message });
        }
        res.json({ updated: true });
    });
});

app.delete("/chapters/:id", (req, res) => {
    db.run("DELETE FROM chapters WHERE id = ?", req.params.id, (err) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ deleted: true });
    });
});

app.get("/chapters/:chapter_id/lessons", (req, res) => {
    db.all("SELECT * FROM lessons WHERE chapter_id = ?", [req.params.chapter_id], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

app.post("/chapters/:chapter_id/lessons", (req, res) => {
    const { name } = req.body;
    db.run("INSERT INTO lessons (name, chapter_id) VALUES (?, ?)", [name, req.params.chapter_id], function (err) {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ id: this.lastID });
    });
});

app.put("/lessons/:id", (req, res) => {
    const { name, visible } = req.body;
    db.run("UPDATE lessons SET name = ?, visible = ? WHERE id = ?", [name, visible, req.params.id], (err) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ updated: true });
    });
});

app.delete("/lessons/:id", (req, res) => {
    db.run("DELETE FROM lessons WHERE id = ?", req.params.id, (err) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ deleted: true });
    });
});

app.get("/lessons/:lesson_id/tasks", (req, res) => {
    db.all("SELECT * FROM tasks WHERE lesson_id = ?", [req.params.lesson_id], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows.map(task => ({ ...task, data: JSON.parse(task.data) })));
    });
});

app.post("/lessons/:lesson_id/tasks", (req, res) => {
    const { name, type, data, points = 0.0 } = req.body;

    db.get("SELECT COUNT(*) as count FROM tasks WHERE lesson_id = ?", [req.params.lesson_id], (err, row) => {
        if (err) return res.status(500).json({ error: err.message });

        const newPosition = row.count;
        db.run(
            "INSERT INTO tasks (name, type, data, lesson_id, position, points) VALUES (?, ?, ?, ?, ?, ?)",
            [name, type, JSON.stringify(data), req.params.lesson_id, newPosition, points],
            function (err) {
                if (err) return res.status(500).json({ error: err.message });
                res.json({ id: this.lastID, position: newPosition, points });
            }
        );
    });
});

app.put("/tasks/:id", (req, res) => {
    const { name, type, data, visible, points } = req.body;
    db.run(
        "UPDATE tasks SET name = ?, type = ?, data = ?, visible = ?, points = ? WHERE id = ?",
        [name, type, JSON.stringify(data), visible, points, req.params.id],
        (err) => {
            if (err) return res.status(500).json({ error: err.message });
            res.json({ updated: true });
        }
    );
});

app.delete("/tasks/:id", (req, res) => {
    db.run("DELETE FROM tasks WHERE id = ?", req.params.id, (err) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ deleted: true });
    });
});

app.put("/lessons/:lesson_id/tasks/reorder", (req, res) => {
    console.log("Reordering tasks:", req.body);

    const { orderedTasks } = req.body;
    if (!Array.isArray(orderedTasks)) {
        return res.status(400).json({ error: "Invalid request format" });
    }

    const queries = orderedTasks.map((task, index) => {
        return new Promise((resolve, reject) => {
            db.run("UPDATE tasks SET position = ? WHERE id = ?", [index, task.id], (err) => {
                if (err) reject(err);
                else resolve();
            });
        });
    });

    Promise.all(queries)
        .then(() => {
            console.log("Tasks successfully reordered");
            res.json({ updated: true });
        })
        .catch((err) => {
            console.error("Error updating tasks:", err);
            res.status(500).json({ error: err.message });
        });
});


const multer = require("multer");
const fs = require("fs");

const upload = multer({ dest: "uploads/" });

app.post("/upload", upload.single("image"), (req, res) => {
    const file = req.file;
    if (!file) return res.status(400).json({ error: "Žiadny súbor nebol nahraný." });

    fs.readFile(file.path, (err, data) => {
        if (err) return res.status(500).json({ error: "Chyba pri čítaní súboru." });

        const base64Image = `data:${file.mimetype};base64,${data.toString("base64")}`;
        fs.unlink(file.path, () => { });

        res.json({ image: base64Image });
    });
});


app.get("/courses/:id", (req, res) => {
    db.get("SELECT * FROM courses WHERE id = ?", [req.params.id], (err, row) => {
        if (err) return res.status(500).json({ error: err.message });
        if (!row) return res.status(404).json({ error: "Kurz nebol nájdený" });
        res.json(row);
    });
});

app.put("/chapters/:chapter_id/lessons/reorder", (req, res) => {
    const { orderedLessons } = req.body;

    const queries = orderedLessons.map((lesson, index) => {
        return new Promise((resolve, reject) => {
            db.run("UPDATE lessons SET position = ? WHERE id = ?", [index, lesson.id], (err) => {
                if (err) reject(err);
                else resolve();
            });
        });
    });

    Promise.all(queries)
        .then(() => res.json({ updated: true }))
        .catch((err) => res.status(500).json({ error: err.message }));
});


app.get("/courses/:course_id/chapters/count", (req, res) => {
    db.get("SELECT COUNT(*) as count FROM chapters WHERE course_id = ?", [req.params.course_id], (err, row) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ count: row.count });
    });
});

app.get("/chapters/:chapter_id/lessons/count", (req, res) => {
    db.get("SELECT COUNT(*) as count FROM lessons WHERE chapter_id = ?", [req.params.chapter_id], (err, row) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ count: row.count });
    });
});


app.get("/lessons/:lesson_id/tasks/count", (req, res) => {
    db.get("SELECT COUNT(*) as count FROM tasks WHERE lesson_id = ?", [req.params.lesson_id], (err, row) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ count: row.count });
    });
});


app.post("/players", (req, res) => {
    const { name, email, user_id } = req.body;

    if (!name || !email || !user_id) {
        return res.status(400).json({ error: "Meno, email a ID sú povinné." });
    }

    db.get("SELECT * FROM players WHERE user_id = ?", [user_id], (err, row) => {
        if (err) {
            return res.status(500).json({ error: err.message });
        }

        if (row) {
            return res.status(400).json({ error: "Player already exists." });
        }

        db.run("INSERT INTO players (name, email, user_id) VALUES (?, ?, ?)", [name, email, user_id], function (err) {
            if (err) {
                return res.status(500).json({ error: err.message });
            }
            res.status(201).json({ id: this.lastID, name, email, user_id });
        });
    });
});


app.get("/players", (req, res) => {
    db.all("SELECT * FROM players", (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});


app.post("/player-scores", (req, res) => {
    const { player_id, task_id, score, attempt_count } = req.body;

    if (typeof player_id !== 'number' || typeof task_id !== 'number' || typeof score !== 'number' || typeof attempt_count !== 'number') {
        return res.status(400).json({ error: 'Invalid input data' });
    }

    const sql = `INSERT INTO player_scores (player_id, task_id, score, attempt_count)
                 VALUES (?, ?, ?, ?)`;

    db.run(sql, [player_id, task_id, score, attempt_count], function (err) {
        if (err) {
            console.error("Chyba pri vkladaní dát do databázy:", err.message);
            return res.status(500).json({ error: 'Database error' });
        }

        res.status(201).json({ message: 'Score added successfully', id: this.lastID });
    });
});

app.get("/player-scores/:player_id", (req, res) => {
    const player_id = req.params.player_id;

    console.log(`Received request for player scores with player_id: ${player_id}`);

    if (isNaN(player_id)) {
        console.log("Invalid player_id input, it must be a number.");
        return res.status(400).json({ error: 'Invalid player ID' });
    }

    console.log(`Validated player_id: ${player_id}`);

    const sql = `SELECT * FROM player_scores WHERE player_id = ?`;

    console.log(`Executing SQL query: ${sql} with player_id: ${player_id}`);

    db.all(sql, [player_id], (err, rows) => {
        if (err) {
            console.error("Error retrieving data from database:", err.message);
            return res.status(500).json({ error: 'Database error' });
        }

        console.log("Fetched player scores from database:", rows);

        res.status(200).json(rows);
    });
});


app.listen(3000, () => console.log("✅ Server beží na http://localhost:3000"));
//app.listen(3000, '0.0.0.0', () => console.log("✅ Server beží na http://0.0.0.0:3000"));
