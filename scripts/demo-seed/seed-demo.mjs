import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

import { applicationDefault, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(SCRIPT_DIR, "../..");
const PLAN_PATH = resolve(REPO_ROOT, "AURA_DEMO_DATA_PLAN.md");
const FIREBASE_RC_PATH = resolve(REPO_ROOT, ".firebaserc");
const GOOGLE_SERVICES_PATH = resolve(REPO_ROOT, "composeApp/google-services.json");

const COLLECTIONS = [
  {
    key: "todos",
    label: "Todos",
    documents: [
      {
        id: "demo-t01",
        data: {
          title: "Preparar examen de Física I",
          description: "Resolver problemas 4–12 y resumir cinemática en una hoja antes del laboratorio.",
          isCompleted: false,
          dueDate: 1789646400000,
        },
      },
      {
        id: "demo-t02",
        data: {
          title: "Revisar Pull Request de autenticación de Pulso",
          description: "Comprobar refresh token, manejo de errores y cobertura del flujo Android.",
          isCompleted: false,
          dueDate: 1789560000000,
        },
      },
      {
        id: "demo-t03",
        data: {
          title: "Laboratorio Web Security — Juice Shop",
          description: "Completar retos de autenticación rota y guardar evidencias para el informe.",
          isCompleted: false,
          dueDate: 1789819200000,
        },
      },
      {
        id: "demo-t04",
        data: {
          title: "Documentar API del proyecto Pulso",
          description: "Actualizar endpoints, payloads y ejemplos de respuesta para el equipo.",
          isCompleted: false,
          dueDate: 1790337600000,
        },
      },
      {
        id: "demo-t05",
        data: {
          title: "Estudiar fundamentos de Application Security",
          description: "Repasar OWASP Top 10, threat modeling y controles de sesión.",
          isCompleted: false,
          dueDate: 1789992000000,
        },
      },
      {
        id: "demo-t06",
        data: {
          title: "Preparar demo de Arquitectura de Software",
          description: "Cerrar diagrama de componentes y anotar trade-offs para la revisión del viernes.",
          isCompleted: false,
          dueDate: 1789732800000,
        },
      },
      {
        id: "demo-t07",
        data: {
          title: "Configurar pipeline CI para Aura",
          description: "Separar lint, unit tests e instrumentación en el workflow de integración.",
          isCompleted: false,
          dueDate: 1790510400000,
        },
      },
      {
        id: "demo-t08",
        data: {
          title: "Resolver ejercicios de estructuras de datos",
          description: "Completar árboles AVL y justificar la complejidad amortizada de cada solución.",
          isCompleted: false,
        },
      },
      {
        id: "demo-t09",
        data: {
          title: "Modelar esquema Firestore de Aura",
          description: "Definir colecciones users/todos, habits, completions y journals.",
          isCompleted: true,
          dueDate: 1788868800000,
        },
      },
      {
        id: "demo-t10",
        data: {
          title: "Cerrar issue de navegación en Pulso",
          description: "Verificar back stack y apertura desde la notificación de finalización.",
          isCompleted: true,
          dueDate: 1789041600000,
        },
      },
      {
        id: "demo-t11",
        data: {
          title: "Entregar informe de Redes II",
          description: "Revisar topología, direccionamiento y pruebas de latencia antes de subirlo.",
          isCompleted: true,
          dueDate: 1788782400000,
        },
      },
      {
        id: "demo-t12",
        data: {
          title: "Revisar cobertura del módulo de hábitos",
          description: "Añadir casos de recurrencia semanal y mensual al conjunto de pruebas.",
          isCompleted: true,
          dueDate: 1789214400000,
        },
      },
      {
        id: "demo-t13",
        data: {
          title: "Publicar notas de la reunión de proyecto",
          description: "Registrar decisiones, responsables y próximos pasos para el equipo.",
          isCompleted: true,
        },
      },
      {
        id: "demo-t14",
        data: {
          title: "Refactorizar validación del formulario de Aura",
          description: "Eliminar duplicación y dejar mensajes claros para estados inválidos.",
          isCompleted: true,
          dueDate: 1788609600000,
        },
      },
    ],
  },
  {
    key: "habits",
    label: "Habits",
    documents: [
      {
        id: "demo-h01",
        data: {
          name: "Revisar fundamentos de Application Security",
          recurrenceType: "DAILY",
          targetCount: 1,
          color: "#7C6AE6",
          createdAt: 1787659200000,
        },
      },
      {
        id: "demo-h02",
        data: {
          name: "Practicar algoritmos antes de clase",
          recurrenceType: "WEEKLY",
          targetCount: 4,
          color: "#6A9AE8",
          createdAt: 1787918400000,
        },
      },
      {
        id: "demo-h03",
        data: {
          name: "Laboratorio Web Security — Juice Shop",
          recurrenceType: "WEEKLY",
          targetCount: 3,
          color: "#E9B949",
          createdAt: 1788264000000,
        },
      },
      {
        id: "demo-h04",
        data: {
          name: "Revisar backlog personal",
          recurrenceType: "MONTHLY",
          targetCount: 8,
          color: "#FF8D70",
          createdAt: 1788350400000,
        },
      },
      {
        id: "demo-h05",
        data: {
          name: "Escribir bitácora técnica de Aura",
          recurrenceType: "DAILY",
          targetCount: 1,
          color: "#51B89E",
          createdAt: 1788609600000,
        },
      },
    ],
  },
  {
    key: "completions",
    label: "Completions",
    documents: [
      {
        id: "demo-h01-2026-09-09",
        data: {
          habitId: "demo-h01",
          completedDate: "2026-09-09",
          completedAt: 1788955200000,
        },
      },
      {
        id: "demo-h01-2026-09-10",
        data: {
          habitId: "demo-h01",
          completedDate: "2026-09-10",
          completedAt: 1789041600000,
        },
      },
      {
        id: "demo-h01-2026-09-11",
        data: {
          habitId: "demo-h01",
          completedDate: "2026-09-11",
          completedAt: 1789128000000,
        },
      },
      {
        id: "demo-h01-2026-09-12",
        data: {
          habitId: "demo-h01",
          completedDate: "2026-09-12",
          completedAt: 1789214400000,
        },
      },
      {
        id: "demo-h01-2026-09-13",
        data: {
          habitId: "demo-h01",
          completedDate: "2026-09-13",
          completedAt: 1789300800000,
        },
      },
      {
        id: "demo-h01-2026-09-14",
        data: {
          habitId: "demo-h01",
          completedDate: "2026-09-14",
          completedAt: 1789387200000,
        },
      },
      {
        id: "demo-h01-2026-09-15",
        data: {
          habitId: "demo-h01",
          completedDate: "2026-09-15",
          completedAt: 1789473600000,
        },
      },
      {
        id: "demo-h02-2026-09-07",
        data: {
          habitId: "demo-h02",
          completedDate: "2026-09-07",
          completedAt: 1788782400000,
        },
      },
      {
        id: "demo-h02-2026-09-08",
        data: {
          habitId: "demo-h02",
          completedDate: "2026-09-08",
          completedAt: 1788868800000,
        },
      },
      {
        id: "demo-h02-2026-09-09",
        data: {
          habitId: "demo-h02",
          completedDate: "2026-09-09",
          completedAt: 1788955200000,
        },
      },
      {
        id: "demo-h02-2026-09-10",
        data: {
          habitId: "demo-h02",
          completedDate: "2026-09-10",
          completedAt: 1789041600000,
        },
      },
      {
        id: "demo-h02-2026-09-14",
        data: {
          habitId: "demo-h02",
          completedDate: "2026-09-14",
          completedAt: 1789387200000,
        },
      },
      {
        id: "demo-h02-2026-09-15",
        data: {
          habitId: "demo-h02",
          completedDate: "2026-09-15",
          completedAt: 1789473600000,
        },
      },
      {
        id: "demo-h03-2026-09-07",
        data: {
          habitId: "demo-h03",
          completedDate: "2026-09-07",
          completedAt: 1788782400000,
        },
      },
      {
        id: "demo-h03-2026-09-08",
        data: {
          habitId: "demo-h03",
          completedDate: "2026-09-08",
          completedAt: 1788868800000,
        },
      },
      {
        id: "demo-h03-2026-09-09",
        data: {
          habitId: "demo-h03",
          completedDate: "2026-09-09",
          completedAt: 1788955200000,
        },
      },
      {
        id: "demo-h03-2026-09-14",
        data: {
          habitId: "demo-h03",
          completedDate: "2026-09-14",
          completedAt: 1789387200000,
        },
      },
      {
        id: "demo-h04-2026-09-02",
        data: {
          habitId: "demo-h04",
          completedDate: "2026-09-02",
          completedAt: 1788350400000,
        },
      },
      {
        id: "demo-h04-2026-09-05",
        data: {
          habitId: "demo-h04",
          completedDate: "2026-09-05",
          completedAt: 1788609600000,
        },
      },
      {
        id: "demo-h04-2026-09-08",
        data: {
          habitId: "demo-h04",
          completedDate: "2026-09-08",
          completedAt: 1788868800000,
        },
      },
      {
        id: "demo-h04-2026-09-11",
        data: {
          habitId: "demo-h04",
          completedDate: "2026-09-11",
          completedAt: 1789128000000,
        },
      },
      {
        id: "demo-h04-2026-09-14",
        data: {
          habitId: "demo-h04",
          completedDate: "2026-09-14",
          completedAt: 1789387200000,
        },
      },
      {
        id: "demo-h05-2026-09-14",
        data: {
          habitId: "demo-h05",
          completedDate: "2026-09-14",
          completedAt: 1789387200000,
        },
      },
    ],
  },
  {
    key: "journals",
    label: "Journals",
    documents: [
      {
        id: "demo-j01",
        data: {
          title: "Una semana de decisiones pequeñas",
          content:
            "Hoy cerré el mapa de trabajo de Aura: ocho tareas pendientes, tres hábitos activos y un espacio claro para el enfoque. La prioridad sigue siendo terminar el laboratorio de seguridad sin sacrificar la documentación.",
          createdAt: 1789473600000,
          updatedAt: 1789473600000,
        },
      },
      {
        id: "demo-j02",
        data: {
          title: "Qué aprendí del laboratorio de seguridad",
          content:
            "En Juice Shop confirmé que una prueba útil no solo encuentra un fallo: también deja una evidencia que otra persona puede repetir. Mañana voy a convertir los hallazgos en una pequeña guía para el equipo.",
          createdAt: 1789214400000,
          updatedAt: 1789214400000,
        },
      },
      {
        id: "demo-j03",
        data: {
          title: "Pulso: menos superficie, más claridad",
          content:
            "Pulso se siente más simple después de separar la sesión de autenticación del resto de la navegación. El siguiente paso es revisar el contrato del refresh token con una prueba de integración.",
          createdAt: 1788868800000,
          updatedAt: 1788868800000,
        },
      },
      {
        id: "demo-j04",
        data: {
          title: "La parte difícil de entender redes",
          content:
            "La parte difícil de Redes II no fue memorizar comandos, sino explicar por qué cada salto existe. Dibujar la topología antes de tocar la configuración me ahorró varias vueltas.",
          createdAt: 1788436800000,
          updatedAt: 1788436800000,
        },
      },
      {
        id: "demo-j05",
        data: {
          title: "Primer borrador de Aura",
          content:
            "El primer borrador de Aura tenía demasiadas tarjetas y muy poco ritmo. Al reducir la jerarquía a una acción principal, el producto empezó a sentirse como una herramienta que podría usar cada día.",
          createdAt: 1787832000000,
          updatedAt: 1787832000000,
        },
      },
      {
        id: "demo-j06",
        data: {
          title: "Ritual de estudio antes de clase",
          content:
            "Antes de la clase de Sistemas Distribuidos preparo una pregunta concreta y dejo el teléfono fuera de alcance. Veinte minutos de lectura enfocada rinden más que una sesión larga interrumpida.",
          createdAt: 1787140800000,
          updatedAt: 1787140800000,
        },
      },
    ],
  },
];

const EXPECTED_COUNTS = Object.fromEntries(
  COLLECTIONS.map((collection) => [collection.key, collection.documents.length]),
);
const TOTAL_DOCUMENTS = Object.values(EXPECTED_COUNTS).reduce((total, count) => total + count, 0);
const VALID_RECURRENCES = new Set(["DAILY", "WEEKLY", "MONTHLY"]);
const VALID_COLORS = new Set(["#FF8D70", "#51B89E", "#6A9AE8", "#7C6AE6", "#E9B949", "#B779D1"]);
const LONG_FIELDS = new Set(["dueDate", "createdAt", "completedAt", "updatedAt"]);

function fail(message) {
  throw new Error(message);
}

function assert(condition, message) {
  if (!condition) {
    fail(message);
  }
}

function assertExactKeys(value, expectedKeys, context) {
  const actualKeys = Object.keys(value).sort();
  const requiredKeys = [...expectedKeys].sort();
  assert(
    JSON.stringify(actualKeys) === JSON.stringify(requiredKeys),
    `${context} tiene campos inesperados o faltantes. Esperados: ${requiredKeys.join(", ")}; recibidos: ${actualKeys.join(", ")}`,
  );
}

function assertNonEmptyString(value, context) {
  assert(typeof value === "string" && value.trim().length > 0, `${context} debe ser texto no vacío.`);
}

function assertLong(value, context) {
  assert(Number.isSafeInteger(value), `${context} debe ser un entero Long seguro.`);
}

function assertValidIsoDate(value, context) {
  assert(/^\d{4}-\d{2}-\d{2}$/.test(value), `${context} debe tener formato YYYY-MM-DD.`);
  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  assert(
    date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day,
    `${context} no es una fecha calendario válida.`,
  );
}

function assertUniqueIds(collection) {
  const ids = collection.documents.map((document) => document.id);
  assert(new Set(ids).size === ids.length, `${collection.label} contiene IDs duplicados.`);
  ids.forEach((id) => assert(/^demo-/.test(id), `${collection.label} contiene un ID que no empieza por demo-: ${id}`));
}

function validateTodos(collection) {
  assert(collection.documents.length === 14, "La colección Todos debe tener exactamente 14 documentos.");
  collection.documents.forEach((document) => {
    const context = `Todo ${document.id}`;
    assertExactKeys(document.data, document.data.dueDate === undefined ? ["title", "description", "isCompleted"] : ["title", "description", "isCompleted", "dueDate"], context);
    assertNonEmptyString(document.data.title, `${context}.title`);
    assert(typeof document.data.isCompleted === "boolean", `${context}.isCompleted debe ser booleano.`);
    if (document.data.dueDate !== undefined) {
      assertLong(document.data.dueDate, `${context}.dueDate`);
    }
  });
}

function validateHabits(collection) {
  assert(collection.documents.length === 5, "La colección Habits debe tener exactamente 5 documentos.");
  collection.documents.forEach((document) => {
    const context = `Habit ${document.id}`;
    assertExactKeys(document.data, ["name", "recurrenceType", "targetCount", "color", "createdAt"], context);
    assertNonEmptyString(document.data.name, `${context}.name`);
    assert(VALID_RECURRENCES.has(document.data.recurrenceType), `${context}.recurrenceType no es DAILY, WEEKLY o MONTHLY.`);
    assert(Number.isSafeInteger(document.data.targetCount), `${context}.targetCount debe ser un entero válido.`);
    const maxTarget = document.data.recurrenceType === "DAILY" ? 1 : document.data.recurrenceType === "WEEKLY" ? 7 : 31;
    assert(document.data.targetCount >= 1 && document.data.targetCount <= maxTarget, `${context}.targetCount está fuera de rango.`);
    if (document.data.recurrenceType === "DAILY") {
      assert(document.data.targetCount === 1, `${context}.targetCount diario debe ser 1.`);
    }
    assert(VALID_COLORS.has(document.data.color), `${context}.color no está permitido por el plan.`);
    assertLong(document.data.createdAt, `${context}.createdAt`);
  });
}

function validateCompletions(collection, habitsCollection) {
  assert(collection.documents.length === 23, "La colección Completions debe tener exactamente 23 documentos.");
  const habitIds = new Set(habitsCollection.documents.map((document) => document.id));
  const pairs = new Set();
  collection.documents.forEach((document) => {
    const context = `Completion ${document.id}`;
    assertExactKeys(document.data, ["habitId", "completedDate", "completedAt"], context);
    assert(habitIds.has(document.data.habitId), `${context}.habitId no corresponde a un hábito demo.`);
    assertValidIsoDate(document.data.completedDate, `${context}.completedDate`);
    assertLong(document.data.completedAt, `${context}.completedAt`);
    const pair = `${document.data.habitId}|${document.data.completedDate}`;
    assert(!pairs.has(pair), `Completions contiene un duplicado habitId + completedDate: ${pair}`);
    pairs.add(pair);
  });
}

function validateJournals(collection) {
  assert(collection.documents.length === 6, "La colección Journals debe tener exactamente 6 documentos.");
  collection.documents.forEach((document) => {
    const context = `Journal ${document.id}`;
    assertExactKeys(document.data, ["title", "content", "createdAt", "updatedAt"], context);
    assertNonEmptyString(document.data.title, `${context}.title`);
    assertNonEmptyString(document.data.content, `${context}.content`);
    assertLong(document.data.createdAt, `${context}.createdAt`);
    assertLong(document.data.updatedAt, `${context}.updatedAt`);
  });
}

function validateDemoData() {
  COLLECTIONS.forEach(assertUniqueIds);
  const todos = COLLECTIONS.find((collection) => collection.key === "todos");
  const habits = COLLECTIONS.find((collection) => collection.key === "habits");
  const completions = COLLECTIONS.find((collection) => collection.key === "completions");
  const journals = COLLECTIONS.find((collection) => collection.key === "journals");

  validateTodos(todos);
  validateHabits(habits);
  validateCompletions(completions, habits);
  validateJournals(journals);
  assert(TOTAL_DOCUMENTS === 48, `El total local debe ser 48, no ${TOTAL_DOCUMENTS}.`);
}

async function readAuraProjectId() {
  const [planText, firebaseRcText, googleServicesText] = await Promise.all([
    readFile(PLAN_PATH, "utf8"),
    readFile(FIREBASE_RC_PATH, "utf8"),
    readFile(GOOGLE_SERVICES_PATH, "utf8"),
  ]);

  const firebaseRc = JSON.parse(firebaseRcText.replace(/^\uFEFF/, ""));
  const googleServices = JSON.parse(googleServicesText.replace(/^\uFEFF/, ""));
  const firebaseRcProjectId = firebaseRc?.projects?.default;
  const appProjectId = googleServices?.project_info?.project_id;

  assert(typeof firebaseRcProjectId === "string" && firebaseRcProjectId.length > 0, "No se encontró el proyecto default en .firebaserc.");
  assert(typeof appProjectId === "string" && appProjectId.length > 0, "No se encontró project_info.project_id en google-services.json.");
  assert(firebaseRcProjectId === appProjectId, `La configuración Firebase de Aura no coincide: .firebaserc=${firebaseRcProjectId}, google-services.json=${appProjectId}.`);
  assert(planText.includes(firebaseRcProjectId), `El proyecto ${firebaseRcProjectId} no coincide con el proyecto mencionado en AURA_DEMO_DATA_PLAN.md.`);

  return firebaseRcProjectId;
}

function requireEnvironment(name) {
  const value = process.env[name];
  assert(typeof value === "string" && value.trim().length > 0, `Falta la variable de entorno ${name}.`);
  return value.trim();
}

async function resolveAdminProjectId(app) {
  if (typeof app.options.projectId === "string" && app.options.projectId.length > 0) {
    return app.options.projectId;
  }

  const credential = app.options.credential;
  if (credential && typeof credential.getProjectId === "function") {
    const projectId = await credential.getProjectId();
    if (typeof projectId === "string" && projectId.length > 0) {
      return projectId;
    }
  }

  fail("Firebase Admin no pudo detectar el project ID usando Application Default Credentials.");
}

function collectionPath(uid, collectionKey) {
  return `users/${uid}/${collectionKey}`;
}

function documentPath(uid, collectionKey, documentId) {
  return `${collectionPath(uid, collectionKey)}/${documentId}`;
}

async function readExistingCounts(db, uid) {
  const entries = await Promise.all(
    COLLECTIONS.map(async (collection) => {
      const snapshot = await db.collection(collectionPath(uid, collection.key)).get();
      return [collection.key, snapshot.size];
    }),
  );
  return Object.fromEntries(entries);
}

function printIdentity(projectId, email, uid) {
  console.log(`PROJECT: ${projectId}`);
  console.log(`EMAIL: ${email}`);
  console.log(`UID: ${uid}`);
}

function printCounts(title, counts) {
  console.log(title);
  COLLECTIONS.forEach((collection) => console.log(`${collection.label}: ${counts[collection.key]}`));
  console.log(`TOTAL: ${Object.values(counts).reduce((total, count) => total + count, 0)}`);
}

function hasExistingDocuments(counts) {
  return COLLECTIONS.some((collection) => counts[collection.key] > 0);
}

function abortIfSeedTargetIsNotEmpty(counts) {
  const occupied = COLLECTIONS.filter((collection) => counts[collection.key] > 0);
  if (occupied.length === 0) {
    return;
  }

  console.error("SEED ABORTED: Firestore ya contiene datos en la cuenta demo.");
  occupied.forEach((collection) => {
    console.error(`${collection.label}: ${counts[collection.key]} documento(s) en users/{UID}/${collection.key}`);
  });
  console.error("No se borró, actualizó ni sobrescribió ningún documento.");
  fail("El seed requiere que las cuatro colecciones estén vacías.");
}

function printRoutes(uid) {
  console.log("Routes:");
  COLLECTIONS.forEach((collection) => {
    collection.documents.forEach((document) => {
      console.log(documentPath(uid, collection.key, document.id));
    });
  });
}

function assertCountsMatchExpected(counts, context) {
  COLLECTIONS.forEach((collection) => {
    assert(counts[collection.key] === EXPECTED_COUNTS[collection.key], `${context}: ${collection.label} debe tener ${EXPECTED_COUNTS[collection.key]}, no ${counts[collection.key]}.`);
  });
  const total = Object.values(counts).reduce((sum, count) => sum + count, 0);
  assert(total === TOTAL_DOCUMENTS, `${context}: TOTAL debe ser ${TOTAL_DOCUMENTS}, no ${total}.`);
}

function assertDocumentDataMatches(actual, expected, context) {
  assert(actual !== undefined, `${context} no existe después del seed.`);
  assertExactKeys(actual, Object.keys(expected), context);
  Object.entries(expected).forEach(([field, value]) => {
    assert(actual[field] === value, `${context}.${field} no coincide con el plan.`);
  });
}

async function verifySeededDocuments(db, uid) {
  for (const collection of COLLECTIONS) {
    const snapshot = await db.collection(collectionPath(uid, collection.key)).get();
    const actualById = new Map(snapshot.docs.map((document) => [document.id, document.data()]));
    collection.documents.forEach((expected) => {
      assertDocumentDataMatches(actualById.get(expected.id), expected.data, documentPath(uid, collection.key, expected.id));
    });
    assert(actualById.size === collection.documents.length, `${collection.label}: el conjunto de IDs no coincide con el plan.`);
  }
}

async function writeDemoData(db, uid) {
  const batch = db.batch();
  COLLECTIONS.forEach((collection) => {
    collection.documents.forEach((document) => {
      // create() garantiza que una aparición concurrente no se sobrescriba.
      batch.create(db.doc(documentPath(uid, collection.key, document.id)), document.data);
    });
  });
  await batch.commit();
}

function parseMode() {
  const args = process.argv.slice(2);
  assert(args.length === 1 && (args[0] === "--dry-run" || args[0] === "--seed"), "Uso: node seed-demo.mjs --dry-run | --seed");
  return args[0] === "--dry-run" ? "dry-run" : "seed";
}

async function main() {
  const mode = parseMode();
  validateDemoData();
  const email = requireEnvironment("AURA_DEMO_EMAIL");
  requireEnvironment("GOOGLE_APPLICATION_CREDENTIALS");
  const expectedProjectId = await readAuraProjectId();

  let app;
  try {
    // Firebase Admin lee GOOGLE_APPLICATION_CREDENTIALS internamente mediante ADC.
    app = initializeApp({ credential: applicationDefault() });
    const detectedProjectId = await resolveAdminProjectId(app);
    assert(
      detectedProjectId === expectedProjectId,
      `El project ID detectado (${detectedProjectId}) no corresponde al Firebase de Aura (${expectedProjectId}).`,
    );

    const auth = getAuth(app);
    const db = getFirestore(app);
    const user = await auth.getUserByEmail(email);
    const uid = user.uid;

    printIdentity(detectedProjectId, email, uid);
    const existingCounts = await readExistingCounts(db, uid);
    printCounts("Existing:", existingCounts);

    if (mode === "dry-run") {
      if (hasExistingDocuments(existingCounts)) {
        console.log("NOTICE: --seed abortaría porque una o más colecciones ya contienen documentos.");
      }
      console.log("To create:");
      COLLECTIONS.forEach((collection) => console.log(`${collection.label}: ${EXPECTED_COUNTS[collection.key]}`));
      console.log(`TOTAL: ${TOTAL_DOCUMENTS}`);
      printRoutes(uid);
      console.log("DRY RUN COMPLETED");
      console.log("NO DATA WAS WRITTEN");
      return;
    }

    abortIfSeedTargetIsNotEmpty(existingCounts);
    await writeDemoData(db, uid);
    const afterCounts = await readExistingCounts(db, uid);
    printCounts("After seed:", afterCounts);
    assertCountsMatchExpected(afterCounts, "Validación posterior");
    await verifySeededDocuments(db, uid);
    console.log("SEED COMPLETED");
    console.log("48 DOCUMENTS CREATED AND VERIFIED");
  } finally {
    if (app) {
      await app.delete();
    }
  }
}

try {
  await main();
} catch (error) {
  const message = error instanceof Error ? error.message : String(error);
  console.error(`ERROR: ${message}`);
  process.exitCode = 1;
}
