package com.pythonide.data.repository

import android.content.Context
import android.os.Environment
import com.pythonide.data.local.AutoSaveConfigDao
import com.pythonide.data.local.AutoSaveConfigEntity
import com.pythonide.data.local.ProjectBackupDao
import com.pythonide.data.local.ProjectBackupEntity
import com.pythonide.data.local.ProjectDao
import com.pythonide.data.local.ProjectEntity
import com.pythonide.data.local.ProjectSessionDao
import com.pythonide.data.local.ProjectSessionEntity
import com.pythonide.data.local.ProjectTemplateDao
import com.pythonide.data.local.ProjectTemplateEntity
import com.pythonide.domain.model.project.AutoSaveConfig
import com.pythonide.domain.model.project.CursorPosition
import com.pythonide.domain.model.project.Project
import com.pythonide.domain.model.project.ProjectBackup
import com.pythonide.domain.model.project.ProjectMetadata
import com.pythonide.domain.model.project.ProjectSession
import com.pythonide.domain.model.project.ProjectStats
import com.pythonide.domain.model.project.ProjectTemplate
import com.pythonide.domain.model.project.SaveResult
import com.pythonide.domain.model.project.ScrollPosition
import com.pythonide.domain.model.project.SessionFile
import com.pythonide.domain.model.project.SplitLayout
import com.pythonide.domain.model.project.TemplateCategory
import com.pythonide.domain.model.project.TemplateFile
import com.pythonide.domain.model.project.TemplateIcon
import com.pythonide.domain.repository.ProjectRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectDao: ProjectDao,
    private val sessionDao: ProjectSessionDao,
    private val backupDao: ProjectBackupDao,
    private val templateDao: ProjectTemplateDao,
    private val autoSaveConfigDao: AutoSaveConfigDao
) : ProjectRepository {

    private val projectsDir = File(context.filesDir, "projects")
    private val backupsDir = File(context.filesDir, "backups")
    private val sessionsDir = File(context.filesDir, "sessions")
    private val templatesDir = File(context.filesDir, "templates")

    init {
        projectsDir.mkdirs()
        backupsDir.mkdirs()
        sessionsDir.mkdirs()
        templatesDir.mkdirs()
    }

    private suspend fun initializeDefaultTemplates() {
        val defaultTemplates = listOf(
            ProjectTemplate(
                id = "blank",
                name = "Blank Project",
                description = "An empty Python project",
                category = TemplateCategory.BLANK,
                icon = TemplateIcon.DEFAULT,
                files = listOf(
                    TemplateFile("main.py", "#!/usr/bin/env python3\n\nif __name__ == '__main__':\n    print('Hello, World!')\n"),
                    TemplateFile("requirements.txt", "# Add your dependencies here\n")
                ),
                directories = listOf("src", "tests"),
                gitignoreContent = "__pycache__/\n*.py[cod]\n*\$py.class\n.env\nvenv/\n.venv/\n",
                isBuiltIn = true
            ),
            ProjectTemplate(
                id = "console_app",
                name = "Console Application",
                description = "A command-line Python application",
                category = TemplateCategory.CONSOLE_APP,
                icon = TemplateIcon.CONSOLE,
                files = listOf(
                    TemplateFile("main.py", "#!/usr/bin/env python3\nimport argparse\nimport sys\n\ndef main():\n    parser = argparse.ArgumentParser(description='My Console App')\n    args = parser.parse_args()\n    print(f'Running console app')\n\nif __name__ == '__main__':\n    main()\n"),
                    TemplateFile("requirements.txt", "# Add your dependencies here\n"),
                    TemplateFile("setup.py", "from setuptools import setup, find_packages\n\nsetup(\n    name='my_console_app',\n    version='1.0.0',\n    packages=find_packages(),\n    entry_points={\n        'console_scripts': [\n            'my-app=main:main',\n        ],\n    },\n)\n")
                ),
                directories = listOf("src", "tests", "docs"),
                gitignoreContent = "__pycache__/\n*.py[cod]\n*.egg-info/\ndist/\nbuild/\n",
                isBuiltIn = true
            ),
            ProjectTemplate(
                id = "web_app",
                name = "Web Application",
                description = "A Flask-based web application",
                category = TemplateCategory.WEB_APP,
                icon = TemplateIcon.WEB,
                files = listOf(
                    TemplateFile("app.py", "from flask import Flask, render_template\n\napp = Flask(__name__)\n\n@app.route('/')\ndef index():\n    return render_template('index.html')\n\nif __name__ == '__main__':\n    app.run(debug=True)\n"),
                    TemplateFile("requirements.txt", "flask>=2.3.0\njinja2>=3.1.0\n"),
                    TemplateFile("config.py", "import os\n\nclass Config:\n    SECRET_KEY = os.environ.get('SECRET_KEY') or 'dev-secret-key'\n    DEBUG = False\n\nclass DevelopmentConfig(Config):\n    DEBUG = True\n\nclass ProductionConfig(Config):\n    DEBUG = False\n\nconfig = {\n    'development': DevelopmentConfig,\n    'production': ProductionConfig,\n    'default': DevelopmentConfig\n}\n")
                ),
                directories = listOf("templates", "static/css", "static/js", "static/images", "tests"),
                gitignoreContent = "__pycache__/\n*.py[cod]\n.env\nvenv/\ninstance/\n*.db\n",
                isBuiltIn = true
            ),
            ProjectTemplate(
                id = "data_science",
                name = "Data Science",
                description = "A data science project with Jupyter support",
                category = TemplateCategory.DATA_SCIENCE,
                icon = TemplateIcon.DATA,
                files = listOf(
                    TemplateFile("notebooks/analysis.ipynb", "{\n  \"cells\": [\n    {\n      \"cell_type\": \"markdown\",\n      \"metadata\": {},\n      \"source\": [\"# Data Analysis\\n\"]\n    }\n  ],\n  \"metadata\": {\n    \"kernelspec\": {\n      \"display_name\": \"Python 3\",\n      \"language\": \"python\",\n      \"name\": \"python3\"\n    }\n  },\n  \"nbformat\": 4,\n  \"nbformat_minor\": 4\n}\n"),
                    TemplateFile("src/data_loader.py", "import pandas as pd\nimport numpy as np\n\ndef load_data(filepath: str) -> pd.DataFrame:\n    \"\"\"Load data from file.\"\"\"\n    return pd.read_csv(filepath)\n\n\ndef preprocess_data(df: pd.DataFrame) -> pd.DataFrame:\n    \"\"\"Preprocess the data.\"\"\"\n    # Add preprocessing steps here\n    return df\n"),
                    TemplateFile("requirements.txt", "pandas>=2.0.0\nnumpy>=1.24.0\njupyter>=1.0.0\nmatplotlib>=3.7.0\nseaborn>=0.12.0\nscikit-learn>=1.3.0\n"),
                    TemplateFile("setup.py", "from setuptools import setup, find_packages\n\nsetup(\n    name='data_science_project',\n    version='1.0.0',\n    packages=find_packages(),\n)\n")
                ),
                directories = listOf("data/raw", "data/processed", "notebooks", "src", "models", "reports/figures"),
                gitignoreContent = "__pycache__/\n*.py[cod]\n.ipynb_checkpoints/\n*.h5\n*.pkl\nvenv/\n",
                isBuiltIn = true
            ),
            ProjectTemplate(
                id = "game",
                name = "Game",
                description = "A Pygame-based game project",
                category = TemplateCategory.GAME,
                icon = TemplateIcon.GAME,
                files = listOf(
                    TemplateFile("main.py", "#!/usr/bin/env python3\nimport pygame\nimport sys\n\ndef main():\n    pygame.init()\n    screen = pygame.display.set_mode((800, 600))\n    pygame.display.set_caption('My Game')\n    clock = pygame.time.Clock()\n    \n    running = True\n    while running:\n        for event in pygame.event.get():\n            if event.type == pygame.QUIT:\n                running = False\n        \n        screen.fill((0, 0, 0))\n        pygame.display.flip()\n        clock.tick(60)\n    \n    pygame.quit()\n    sys.exit()\n\nif __name__ == '__main__':\n    main()\n"),
                    TemplateFile("requirements.txt", "pygame>=2.5.0\n"),
                    TemplateFile("src/game.py", "class Game:\n    def __init__(self):\n        self.running = True\n    \n    def handle_events(self, events):\n        pass\n    \n    def update(self):\n        pass\n    \n    def render(self, screen):\n        pass\n")
                ),
                directories = listOf("src", "assets/images", "assets/sounds", "assets/fonts", "tests"),
                gitignoreContent = "__pycache__/\n*.py[cod]\nvenv/\nbuild/\ndist/\n",
                isBuiltIn = true
            ),
            ProjectTemplate(
                id = "api",
                name = "REST API",
                description = "A FastAPI-based REST API",
                category = TemplateCategory.API,
                icon = TemplateIcon.API,
                files = listOf(
                    TemplateFile("main.py", "from fastapi import FastAPI\nfrom pydantic import BaseModel\n\napp = FastAPI()\n\nclass Item(BaseModel):\n    name: str\n    description: str = None\n    price: float\n    tax: float = None\n\n@app.get('/')\ndef read_root():\n    return {'Hello': 'World'}\n\n@app.get('/items/{item_id}')\ndef read_item(item_id: int, q: str = None):\n    return {'item_id': item_id, 'q': q}\n\n@app.post('/items/')\ndef create_item(item: Item):\n    return item\n"),
                    TemplateFile("requirements.txt", "fastapi>=0.100.0\nuvicorn>=0.23.0\npydantic>=2.0.0\n"),
                    TemplateFile("config.py", "from pydantic_settings import BaseSettings\n\nclass Settings(BaseSettings):\n    app_name: str = 'My API'\n    debug: bool = False\n    database_url: str = 'sqlite:///./app.db'\n    \n    class Config:\n        env_file = '.env'\n\nsettings = Settings()\n"),
                    TemplateFile("routers/items.py", "from fastapi import APIRouter\n\nrouter = APIRouter()\n\n@router.get('/items/')\nasync def read_items():\n    return [{'name': 'Foo'}]\n\n@router.get('/items/{item_id}')\nasync def read_item(item_id: int):\n    return {'item_id': item_id}\n")
                ),
                directories = listOf("routers", "models", "schemas", "services", "tests"),
                gitignoreContent = "__pycache__/\n*.py[cod]\n.env\nvenv/\n*.db\n",
                isBuiltIn = true
            ),
            ProjectTemplate(
                id = "automation",
                name = "Automation",
                description = "A task automation project",
                category = TemplateCategory.AUTOMATION,
                icon = TemplateIcon.GEAR,
                files = listOf(
                    TemplateFile("main.py", "#!/usr/bin/env python3\nimport argparse\nimport logging\n\nlogging.basicConfig(level=logging.INFO)\nlogger = logging.getLogger(__name__)\n\ndef main():\n    parser = argparse.ArgumentParser(description='Task Automation')\n    parser.add_argument('--task', type=str, help='Task to run')\n    args = parser.parse_args()\n    \n    logger.info(f'Running task: {args.task}')\n    # Add your automation logic here\n\nif __name__ == '__main__':\n    main()\n"),
                    TemplateFile("requirements.txt", "# Add your dependencies here\nschedule>=1.2.0\nrequests>=2.31.0\n"),
                    TemplateFile("tasks/file_manager.py", "import os\nimport shutil\nfrom pathlib import Path\n\ndef organize_files(directory: str):\n    \"\"\"Organize files by extension.\"\"\"\n    for file in Path(directory).iterdir():\n        if file.is_file():\n            ext = file.suffix[1:].lower()\n            dest = Path(directory) / ext\n            dest.mkdir(exist_ok=True)\n            shutil.move(str(file), str(dest / file.name))\n")
                ),
                directories = listOf("tasks", "config", "logs", "tests"),
                gitignoreContent = "__pycache__/\n*.py[cod]\n.env\nvenv/\nlogs/\n",
                isBuiltIn = true
            ),
            ProjectTemplate(
                id = "testing",
                name = "Testing Project",
                description = "A project with pytest testing setup",
                category = TemplateCategory.TESTING,
                icon = TemplateIcon.TEST,
                files = listOf(
                    TemplateFile("src/calculator.py", "def add(a: float, b: float) -> float:\n    return a + b\n\ndef subtract(a: float, b: float) -> float:\n    return a - b\n\ndef multiply(a: float, b: float) -> float:\n    return a * b\n\ndef divide(a: float, b: float) -> float:\n    if b == 0:\n        raise ValueError('Cannot divide by zero')\n    return a / b\n"),
                    TemplateFile("tests/test_calculator.py", "import pytest\nfrom src.calculator import add, subtract, multiply, divide\n\ndef test_add():\n    assert add(2, 3) == 5\n    assert add(-1, 1) == 0\n    assert add(0, 0) == 0\n\ndef test_subtract():\n    assert subtract(5, 3) == 2\n    assert subtract(0, 0) == 0\n\ndef test_multiply():\n    assert multiply(2, 3) == 6\n    assert multiply(-1, 1) == -1\n\ndef test_divide():\n    assert divide(6, 3) == 2\n    assert divide(10, 2) == 5\n    with pytest.raises(ValueError):\n        divide(1, 0)\n"),
                    TemplateFile("requirements.txt", "pytest>=7.4.0\npytest-cov>=4.1.0\n"),
                    TemplateFile("pytest.ini", "[pytest]\ntestpaths = tests\npython_files = test_*.py\npython_functions = test_*\naddopts = -v --cov=src\n")
                ),
                directories = listOf("src", "tests", "tests/fixtures"),
                gitignoreContent = "__pycache__/\n*.py[cod]\n.pytest_cache/\n.coverage\nhtmlcov/\nvenv/\n",
                isBuiltIn = true
            )
        )

        defaultTemplates.forEach { template ->
            val entity = ProjectTemplateEntity(
                id = template.id,
                name = template.name,
                description = template.description,
                category = template.category.name,
                icon = template.icon.name,
                files = serializeTemplateFiles(template.files),
                directories = JSONArray(template.directories).toString(),
                gitignoreContent = template.gitignoreContent,
                requirementsContent = template.requirementsContent,
                isBuiltIn = template.isBuiltIn
            )
            templateDao.insertTemplate(entity)
        }
    }

    override fun getProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { projectEntityToDomain(it) }
        }
    }

    override fun getProjectById(id: String): Flow<Project?> {
        return projectDao.getProjectById(id).map { it?.let { entity -> projectEntityToDomain(entity) } }
    }

    override fun getRecentProjects(): Flow<List<Project>> {
        return projectDao.getRecentProjects().map { entities ->
            entities.map { projectEntityToDomain(it) }
        }
    }

    override fun getFavoriteProjects(): Flow<List<Project>> {
        return projectDao.getFavoriteProjects().map { entities ->
            entities.map { projectEntityToDomain(it) }
        }
    }

    override suspend fun createProject(
        name: String,
        description: String,
        templateId: String?,
        rootPath: String?,
        metadata: ProjectMetadata
    ): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val projectRoot = rootPath ?: File(projectsDir, name).absolutePath
            val projectDir = File(projectRoot)

            if (projectDir.exists()) {
                return@withContext Result.failure(Exception("Project directory already exists"))
            }

            projectDir.mkdirs()

            val project = Project(
                id = UUID.randomUUID().toString(),
                name = name,
                rootPath = projectRoot,
                description = description,
                templateId = templateId,
                metadata = metadata
            )

            if (templateId != null) {
                val template = templateDao.getTemplateById(templateId).firstOrNull()
                if (template != null) {
                    applyTemplate(projectDir, templateEntityToDomain(template))
                }
            }

            val entity = projectToEntity(project)
            projectDao.insertProject(entity)

            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun applyTemplate(projectDir: File, template: ProjectTemplate) {
        template.directories.forEach { dir ->
            File(projectDir, dir).mkdirs()
        }

        template.files.forEach { templateFile ->
            val file = File(projectDir, templateFile.path)
            file.parentFile?.mkdirs()
            file.writeText(templateFile.content)
            if (templateFile.isExecutable) {
                file.setExecutable(true)
            }
        }

        template.gitignoreContent?.let { content ->
            File(projectDir, ".gitignore").writeText(content)
        }

        template.requirementsContent?.let { content ->
            val requirementsFile = File(projectDir, "requirements.txt")
            if (!requirementsFile.exists()) {
                requirementsFile.writeText(content)
            }
        }
    }

    override suspend fun openProject(projectId: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val entity = projectDao.getProjectById(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Project not found"))

            val updatedEntity = entity.copy(lastAccessedAt = System.currentTimeMillis())
            projectDao.updateProject(updatedEntity)

            Result.success(projectEntityToDomain(updatedEntity))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun closeProject(projectId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Project not found"))

            val updatedProject = project.copy(lastAccessedAt = System.currentTimeMillis())
            projectDao.updateProject(updatedProject)

            val session = sessionDao.getSession(projectId).firstOrNull()
            if (session != null) {
                val updatedSession = session.copy(lastSavedAt = System.currentTimeMillis())
                sessionDao.updateSession(updatedSession)
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProject(projectId: String, deleteFiles: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Project not found"))

            if (deleteFiles) {
                val projectDir = File(project.rootPath)
                if (projectDir.exists()) {
                    projectDir.deleteRecursively()
                }
            }

            projectDao.deleteProjectById(projectId)
            sessionDao.deleteSessionByProjectId(projectId)
            backupDao.getBackups(projectId).firstOrNull()?.forEach { backup ->
                val backupFile = File(backup.backupPath)
                if (backupFile.exists()) {
                    backupFile.delete()
                }
                backupDao.deleteBackupById(backup.id)
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameProject(projectId: String, newName: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Project not found"))

            val oldDir = File(project.rootPath)
            val newDir = File(oldDir.parent, newName)

            if (newDir.exists()) {
                return@withContext Result.failure(Exception("A project with that name already exists"))
            }

            if (oldDir.exists()) {
                val renamed = oldDir.renameTo(newDir)
                if (!renamed) {
                    return@withContext Result.failure(Exception("Failed to rename project directory"))
                }
            }

            val updatedProject = project.copy(
                name = newName,
                rootPath = newDir.absolutePath,
                lastAccessedAt = System.currentTimeMillis()
            )
            projectDao.updateProject(updatedProject)

            Result.success(projectEntityToDomain(updatedProject))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProject(project: Project): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val updatedProject = project.copy(lastAccessedAt = System.currentTimeMillis())
            projectDao.updateProject(projectToEntity(updatedProject))
            Result.success(updatedProject)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProjectMetadata(projectId: String, metadata: ProjectMetadata): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Project not found"))

            val updatedProject = projectEntityToDomain(project).copy(
                metadata = metadata,
                lastAccessedAt = System.currentTimeMillis()
            )
            projectDao.updateProject(projectToEntity(updatedProject))

            Result.success(updatedProject)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(projectId: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Project not found"))

            val newFavorite = !project.isFavorite
            projectDao.toggleFavorite(projectId, newFavorite)

            val updatedProject = project.copy(
                isFavorite = newFavorite,
                lastAccessedAt = System.currentTimeMillis()
            )
            projectDao.updateProject(updatedProject)

            Result.success(projectEntityToDomain(updatedProject))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addTag(projectId: String, tag: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Project not found"))

            val currentTags = project.tags.split(",").filter { it.isNotEmpty() }.toMutableList()
            if (!currentTags.contains(tag)) {
                currentTags.add(tag)
            }

            val updatedProject = project.copy(
                tags = currentTags.joinToString(","),
                lastAccessedAt = System.currentTimeMillis()
            )
            projectDao.updateProject(updatedProject)

            Result.success(projectEntityToDomain(updatedProject))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeTag(projectId: String, tag: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Project not found"))

            val currentTags = project.tags.split(",").filter { it.isNotEmpty() }.toMutableList()
            currentTags.remove(tag)

            val updatedProject = project.copy(
                tags = currentTags.joinToString(","),
                lastAccessedAt = System.currentTimeMillis()
            )
            projectDao.updateProject(updatedProject)

            Result.success(projectEntityToDomain(updatedProject))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveProject(projectId: String): Result<SaveResult> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Project not found"))

            val projectDir = File(project.rootPath)
            if (!projectDir.exists()) {
                return@withContext Result.failure(Exception("Project directory not found"))
            }

            val session = sessionDao.getSession(projectId).firstOrNull()
            if (session != null) {
                val updatedSession = session.copy(lastSavedAt = System.currentTimeMillis())
                sessionDao.updateSession(updatedSession)
            }

            val stats = calculateProjectStats(projectDir)
            val updatedProject = projectEntityToDomain(project).copy(
                lastSavedAt = System.currentTimeMillis(),
                lastAccessedAt = System.currentTimeMillis(),
                fileCount = stats.totalFiles,
                totalSize = stats.totalSize
            )
            projectDao.updateProject(projectToEntity(updatedProject))

            Result.success(SaveResult.SUCCESS)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveProjectAs(projectId: String, newPath: String, newName: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Project not found"))

            val sourceDir = File(project.rootPath)
            val destDir = File(newPath)

            if (destDir.exists()) {
                return@withContext Result.failure(Exception("Destination directory already exists"))
            }

            sourceDir.copyRecursively(destDir)

            val newProject = Project(
                id = UUID.randomUUID().toString(),
                name = newName,
                rootPath = destDir.absolutePath,
                description = project.description,
                templateId = project.templateId,
                metadata = projectEntityToDomain(project).metadata,
                tags = projectEntityToDomain(project).tags
            )

            projectDao.insertProject(projectToEntity(newProject))

            Result.success(newProject)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveAllProjects(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            projectDao.getAllProjects().firstOrNull()?.forEach { project ->
                val result = saveProject(project.id)
                if (result.isSuccess && result.getOrNull() == SaveResult.SUCCESS) {
                    count++
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private var templatesInitialized = false

    override suspend fun getTemplates(): Flow<List<ProjectTemplate>> {
        if (!templatesInitialized) {
            initializeDefaultTemplates()
            templatesInitialized = true
        }
        return templateDao.getAllTemplates().map { entities ->
            entities.map { templateEntityToDomain(it) }
        }
    }

    override suspend fun getTemplateById(templateId: String): ProjectTemplate? {
        return templateDao.getTemplateById(templateId).firstOrNull()?.let { templateEntityToDomain(it) }
    }

    override suspend fun createProjectFromTemplate(
        templateId: String,
        projectName: String,
        destinationPath: String
    ): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val templateEntity = templateDao.getTemplateById(templateId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Template not found"))
            val template = templateEntityToDomain(templateEntity)

            val projectDir = File(destinationPath, projectName)
            if (projectDir.exists()) {
                return@withContext Result.failure(Exception("Directory already exists"))
            }

            projectDir.mkdirs()
            applyTemplate(projectDir, template)

            val project = Project(
                id = UUID.randomUUID().toString(),
                name = projectName,
                rootPath = projectDir.absolutePath,
                templateId = templateId,
                metadata = ProjectMetadata()
            )

            projectDao.insertProject(projectToEntity(project))

            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCustomTemplate(template: ProjectTemplate): Result<ProjectTemplate> = withContext(Dispatchers.IO) {
        try {
            val entity = templateToEntity(template.copy(
                id = UUID.randomUUID().toString(),
                isBuiltIn = false
            ))
            templateDao.insertTemplate(entity)
            Result.success(templateEntityToDomain(entity))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTemplate(templateId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val templateEntity = templateDao.getTemplateById(templateId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Template not found"))
            val template = templateEntityToDomain(templateEntity)

            if (template.isBuiltIn) {
                return@withContext Result.failure(Exception("Cannot delete built-in template"))
            }

            templateDao.deleteTemplateById(templateId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSession(projectId: String): Flow<ProjectSession?> {
        return sessionDao.getSession(projectId).map { entity ->
            entity?.let { sessionEntityToDomain(it) }
        }
    }

    override suspend fun saveSession(session: ProjectSession): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            sessionDao.insertSession(sessionToEntity(session))
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSessionFile(projectId: String, fileId: String, isModified: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val session = sessionDao.getSession(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Session not found"))

            val openFiles = deserializeSessionFiles(session.openFiles).toMutableList()
            val updatedFiles = openFiles.map { file ->
                if (file.fileId == fileId) {
                    file.copy(isModified = isModified)
                } else {
                    file
                }
            }

            val updatedSession = session.copy(openFiles = serializeSessionFiles(updatedFiles))
            sessionDao.updateSession(updatedSession)

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addOpenFile(projectId: String, path: String, name: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val session = sessionDao.getSession(projectId).firstOrNull()
                ?: ProjectSessionEntity(
                    projectId = projectId,
                    openFiles = "[]",
                    activeFileId = null,
                    cursorPositions = "{}",
                    scrollPositions = "{}",
                    splitLayout = SplitLayout.NONE.name,
                    terminalHistory = "[]",
                    lastSavedAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )

            val fileId = path.hashCode().toString()
            val openFiles = deserializeSessionFiles(session.openFiles).toMutableList()

            if (openFiles.none { it.fileId == fileId }) {
                openFiles.add(SessionFile(
                    fileId = fileId,
                    path = path,
                    name = name
                ))
            }

            val updatedSession = session.copy(
                openFiles = serializeSessionFiles(openFiles),
                activeFileId = fileId
            )
            sessionDao.insertSession(updatedSession)

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeOpenFile(projectId: String, fileId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val session = sessionDao.getSession(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Session not found"))

            val openFiles = deserializeSessionFiles(session.openFiles).toMutableList()
            openFiles.removeAll { it.fileId == fileId }

            val updatedSession = session.copy(
                openFiles = serializeSessionFiles(openFiles),
                activeFileId = if (session.activeFileId == fileId) {
                    openFiles.lastOrNull()?.fileId
                } else {
                    session.activeFileId
                }
            )
            sessionDao.updateSession(updatedSession)

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearSession(projectId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            sessionDao.deleteSessionByProjectId(projectId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreSession(): Result<ProjectSession?> = withContext(Dispatchers.IO) {
        try {
            val lastSession = projectDao.getRecentProjects(1).firstOrNull()?.firstOrNull()
            if (lastSession != null) {
                val session = sessionDao.getSession(lastSession.id).firstOrNull()
                Result.success(session?.let { sessionEntityToDomain(it) })
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAutoSaveConfig(): Flow<AutoSaveConfig> {
        return autoSaveConfigDao.getConfig().map { entity ->
            entity?.let { autoSaveConfigEntityToDomain(it) } ?: AutoSaveConfig()
        }
    }

    override suspend fun updateAutoSaveConfig(config: AutoSaveConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val entity = autoSaveConfigToEntity(config)
            autoSaveConfigDao.insertConfig(entity)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun performAutoSave(projectId: String): Result<SaveResult> = withContext(Dispatchers.IO) {
        try {
            val configEntity = autoSaveConfigDao.getConfig().firstOrNull()
            val config = configEntity?.let { autoSaveConfigEntityToDomain(it) } ?: AutoSaveConfig()
            if (!config.enabled) {
                return@withContext Result.success(SaveResult.SUCCESS)
            }

            saveProject(projectId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createBackup(projectId: String, description: String): Result<ProjectBackup> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Project not found"))

            val projectDir = File(project.rootPath)
            if (!projectDir.exists()) {
                return@withContext Result.failure(Exception("Project directory not found"))
            }

            val backupFileName = "${project.name}_${System.currentTimeMillis()}.zip"
            val backupFile = File(backupsDir, backupFileName)

            createZipArchive(projectDir, backupFile)

            val stats = calculateProjectStats(projectDir)
            val backup = ProjectBackup(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                projectName = project.name,
                backupPath = backupFile.absolutePath,
                size = backupFile.length(),
                fileCount = stats.totalFiles,
                description = description
            )

            backupDao.insertBackup(backupToEntity(backup))

            Result.success(backup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBackups(projectId: String): Flow<List<ProjectBackup>> {
        return backupDao.getBackups(projectId).map { entities ->
            entities.map { backupEntityToDomain(it) }
        }
    }

    override suspend fun restoreBackup(backupId: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val backupEntity = backupDao.getBackupById(backupId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Backup not found"))

            val backupFile = File(backupEntity.backupPath)
            if (!backupFile.exists()) {
                return@withContext Result.failure(Exception("Backup file not found"))
            }

            val restoreDir = File(projectsDir, "${backupEntity.projectName}_restored_${System.currentTimeMillis()}")
            restoreDir.mkdirs()

            extractZipArchive(backupFile, restoreDir)

            val project = Project(
                id = UUID.randomUUID().toString(),
                name = "${backupEntity.projectName}_restored",
                rootPath = restoreDir.absolutePath,
                description = "Restored from backup",
                lastSavedAt = System.currentTimeMillis()
            )

            projectDao.insertProject(projectToEntity(project))

            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteBackup(backupId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val backup = backupDao.getBackupById(backupId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Backup not found"))

            val backupFile = File(backup.backupPath)
            if (backupFile.exists()) {
                backupFile.delete()
            }

            backupDao.deleteBackupById(backupId)

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportBackup(backupId: String, exportPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backup = backupDao.getBackupById(backupId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Backup not found"))

            val sourceFile = File(backup.backupPath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("Backup file not found"))
            }

            val destFile = File(exportPath)
            sourceFile.copyTo(destFile, overwrite = true)

            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importBackup(backupPath: String): Result<ProjectBackup> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(backupPath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("Backup file not found"))
            }

            val destFile = File(backupsDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)

            val backup = ProjectBackup(
                id = UUID.randomUUID().toString(),
                projectId = "imported",
                projectName = sourceFile.nameWithoutExtension,
                backupPath = destFile.absolutePath,
                size = destFile.length()
            )

            backupDao.insertBackup(backupToEntity(backup))

            Result.success(backup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProjectStats(projectId: String): Result<ProjectStats> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(projectId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Project not found"))

            val projectDir = File(project.rootPath)
            if (!projectDir.exists()) {
                return@withContext Result.failure(Exception("Project directory not found"))
            }

            val stats = calculateProjectStats(projectDir)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateProjectStats(projectDir: File): ProjectStats {
        var totalFiles = 0
        var pythonFiles = 0
        var totalLines = 0
        var totalSize = 0L
        var lastModified = 0L
        val dependencies = mutableListOf<String>()

        projectDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                totalFiles++
                totalSize += file.length()
                val modified = file.lastModified()
                if (modified > lastModified) {
                    lastModified = modified
                }

                if (file.extension == "py") {
                    pythonFiles++
                    try {
                        totalLines += file.readLines().size
                    } catch (_: Exception) {}
                }

                if (file.name == "requirements.txt") {
                    try {
                        file.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.forEach {
                            dependencies.add(it)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        return ProjectStats(
            totalFiles = totalFiles,
            pythonFiles = pythonFiles,
            totalLines = totalLines,
            totalSize = totalSize,
            lastModified = lastModified,
            dependencies = dependencies
        )
    }

    private fun createZipArchive(sourceDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(sourceDir).path
                    zipOut.putNextEntry(ZipEntry(entryName))
                    FileInputStream(file).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }
        }
    }

    private fun extractZipArchive(zipFile: File, destDir: File) {
        val destCanonical = destDir.canonicalPath + File.separator
        ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val filePath = File(destDir, entry.name)
                // Zip-Slip protection
                if (!filePath.canonicalPath.startsWith(destCanonical)) {
                    throw SecurityException("Zip entry outside target dir: ${entry.name}")
                }
                if (entry.isDirectory) {
                    filePath.mkdirs()
                } else {
                    filePath.parentFile?.mkdirs()
                    FileOutputStream(filePath).use { output ->
                        zipIn.copyTo(output)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }

    private fun serializeTemplateFiles(files: List<TemplateFile>): String {
        val jsonArray = JSONArray()
        files.forEach { file ->
            val jsonObject = JSONObject()
            jsonObject.put("path", file.path)
            jsonObject.put("content", file.content)
            jsonObject.put("isExecutable", file.isExecutable)
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    private fun deserializeTemplateFiles(json: String): List<TemplateFile> {
        val files = mutableListOf<TemplateFile>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                files.add(TemplateFile(
                    path = jsonObject.getString("path"),
                    content = jsonObject.getString("content"),
                    isExecutable = jsonObject.optBoolean("isExecutable", false)
                ))
            }
        } catch (_: Exception) {}
        return files
    }

    private fun serializeSessionFiles(files: List<SessionFile>): String {
        val jsonArray = JSONArray()
        files.forEach { file ->
            val jsonObject = JSONObject()
            jsonObject.put("fileId", file.fileId)
            jsonObject.put("path", file.path)
            jsonObject.put("name", file.name)
            jsonObject.put("isModified", file.isModified)
            jsonObject.put("openedAt", file.openedAt)
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    private fun deserializeSessionFiles(json: String): List<SessionFile> {
        val files = mutableListOf<SessionFile>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                files.add(SessionFile(
                    fileId = jsonObject.getString("fileId"),
                    path = jsonObject.getString("path"),
                    name = jsonObject.getString("name"),
                    isModified = jsonObject.optBoolean("isModified", false),
                    openedAt = jsonObject.optLong("openedAt", System.currentTimeMillis())
                ))
            }
        } catch (_: Exception) {}
        return files
    }

    private fun serializeCursorPosition(position: CursorPosition): String {
        val jsonObject = JSONObject()
        jsonObject.put("line", position.line)
        jsonObject.put("column", position.column)
        return jsonObject.toString()
    }

    private fun deserializeCursorPosition(json: String): CursorPosition {
        val jsonObject = JSONObject(json)
        return CursorPosition(
            line = jsonObject.getInt("line"),
            column = jsonObject.getInt("column")
        )
    }

    private fun serializeScrollPosition(position: ScrollPosition): String {
        val jsonObject = JSONObject()
        jsonObject.put("x", position.x.toDouble())
        jsonObject.put("y", position.y.toDouble())
        return jsonObject.toString()
    }

    private fun deserializeScrollPosition(json: String): ScrollPosition {
        val jsonObject = JSONObject(json)
        return ScrollPosition(
            x = jsonObject.getDouble("x").toFloat(),
            y = jsonObject.getDouble("y").toFloat()
        )
    }

    private fun projectEntityToDomain(entity: ProjectEntity) = Project(
        id = entity.id,
        name = entity.name,
        rootPath = entity.rootPath,
        description = entity.description,
        templateId = entity.templateId,
        createdAt = entity.createdAt,
        lastAccessedAt = entity.lastAccessedAt,
        lastSavedAt = entity.lastSavedAt,
        isFavorite = entity.isFavorite,
        fileCount = entity.fileCount,
        totalSize = entity.totalSize,
        metadata = ProjectMetadata(
            pythonVersion = entity.pythonVersion,
            interpreterPath = entity.interpreterPath,
            author = entity.author,
            version = entity.version,
            license = entity.license,
            dependencies = entity.dependencies.split(",").filter { it.isNotEmpty() },
            gitRepository = entity.gitRepository,
            customProperties = deserializeCustomProperties(entity.customProperties)
        ),
        tags = entity.tags.split(",").filter { it.isNotEmpty() }
    )

    private fun projectToEntity(project: Project) = ProjectEntity(
        id = project.id,
        name = project.name,
        rootPath = project.rootPath,
        description = project.description,
        templateId = project.templateId,
        createdAt = project.createdAt,
        lastAccessedAt = project.lastAccessedAt,
        lastSavedAt = project.lastSavedAt,
        isFavorite = project.isFavorite,
        fileCount = project.fileCount,
        totalSize = project.totalSize,
        tags = project.tags.joinToString(","),
        pythonVersion = project.metadata.pythonVersion,
        interpreterPath = project.metadata.interpreterPath,
        author = project.metadata.author,
        version = project.metadata.version,
        license = project.metadata.license,
        dependencies = project.metadata.dependencies.joinToString(","),
        gitRepository = project.metadata.gitRepository,
        customProperties = serializeCustomProperties(project.metadata.customProperties)
    )

    private fun templateEntityToDomain(entity: ProjectTemplateEntity) = ProjectTemplate(
        id = entity.id,
        name = entity.name,
        description = entity.description,
        category = try { TemplateCategory.valueOf(entity.category) } catch (_: Exception) { TemplateCategory.CUSTOM },
        icon = try { TemplateIcon.valueOf(entity.icon) } catch (_: Exception) { TemplateIcon.DEFAULT },
        files = deserializeTemplateFiles(entity.files),
        directories = try {
            val array = JSONArray(entity.directories)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: Exception) { emptyList() },
        gitignoreContent = entity.gitignoreContent,
        requirementsContent = entity.requirementsContent,
        isBuiltIn = entity.isBuiltIn
    )

    private fun templateToEntity(template: ProjectTemplate) = ProjectTemplateEntity(
        id = template.id,
        name = template.name,
        description = template.description,
        category = template.category.name,
        icon = template.icon.name,
        files = serializeTemplateFiles(template.files),
        directories = JSONArray(template.directories).toString(),
        gitignoreContent = template.gitignoreContent,
        requirementsContent = template.requirementsContent,
        isBuiltIn = template.isBuiltIn
    )

    private fun sessionEntityToDomain(entity: ProjectSessionEntity) = ProjectSession(
        projectId = entity.projectId,
        openFiles = deserializeSessionFiles(entity.openFiles),
        activeFileId = entity.activeFileId,
        cursorPositions = try {
            val jsonObject = JSONObject(entity.cursorPositions)
            jsonObject.keys().asSequence().associateWith { key ->
                deserializeCursorPosition(jsonObject.getString(key))
            }
        } catch (_: Exception) { emptyMap() },
        scrollPositions = try {
            val jsonObject = JSONObject(entity.scrollPositions)
            jsonObject.keys().asSequence().associateWith { key ->
                deserializeScrollPosition(jsonObject.getString(key))
            }
        } catch (_: Exception) { emptyMap() },
        splitLayout = try { SplitLayout.valueOf(entity.splitLayout) } catch (_: Exception) { SplitLayout.NONE },
        terminalHistory = try {
            val array = JSONArray(entity.terminalHistory)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: Exception) { emptyList() },
        lastSavedAt = entity.lastSavedAt,
        createdAt = entity.createdAt
    )

    private fun sessionToEntity(session: ProjectSession) = ProjectSessionEntity(
        projectId = session.projectId,
        openFiles = serializeSessionFiles(session.openFiles),
        activeFileId = session.activeFileId,
        cursorPositions = JSONObject(session.cursorPositions.mapValues { serializeCursorPosition(it.value) }).toString(),
        scrollPositions = JSONObject(session.scrollPositions.mapValues { serializeScrollPosition(it.value) }).toString(),
        splitLayout = session.splitLayout.name,
        terminalHistory = JSONArray(session.terminalHistory).toString(),
        lastSavedAt = session.lastSavedAt,
        createdAt = session.createdAt
    )

    private fun backupEntityToDomain(entity: ProjectBackupEntity) = ProjectBackup(
        id = entity.id,
        projectId = entity.projectId,
        projectName = entity.projectName,
        backupPath = entity.backupPath,
        createdAt = entity.createdAt,
        size = entity.size,
        fileCount = entity.fileCount,
        description = entity.description
    )

    private fun backupToEntity(backup: ProjectBackup) = ProjectBackupEntity(
        id = backup.id,
        projectId = backup.projectId,
        projectName = backup.projectName,
        backupPath = backup.backupPath,
        createdAt = backup.createdAt,
        size = backup.size,
        fileCount = backup.fileCount,
        description = backup.description
    )

    private fun autoSaveConfigEntityToDomain(entity: AutoSaveConfigEntity) = AutoSaveConfig(
        enabled = entity.enabled,
        intervalMs = entity.intervalMs,
        saveOnClose = entity.saveOnClose,
        saveOnSwitch = entity.saveOnSwitch,
        maxAutoSaves = entity.maxAutoSaves
    )

    private fun autoSaveConfigToEntity(config: AutoSaveConfig) = AutoSaveConfigEntity(
        enabled = config.enabled,
        intervalMs = config.intervalMs,
        saveOnClose = config.saveOnClose,
        saveOnSwitch = config.saveOnSwitch,
        maxAutoSaves = config.maxAutoSaves
    )

    private fun serializeCustomProperties(properties: Map<String, String>): String {
        return JSONObject(properties).toString()
    }

    private fun deserializeCustomProperties(json: String): Map<String, String> {
        return try {
            val jsonObject = JSONObject(json)
            jsonObject.keys().asSequence().associateWith { key ->
                jsonObject.getString(key)
            }
        } catch (_: Exception) { emptyMap() }
    }

    private suspend fun <T> Flow<T>.firstOrNull(): T? {
        var result: T? = null
        this.take(1).collect { result = it }
        return result
    }
}
