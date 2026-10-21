using LicenseHub.UnityDemo;
using TMPro;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.InputSystem.UI;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

namespace LicenseHub.UnityDemo.Editor
{
    [InitializeOnLoad]
    public static class LicenseHubDemoSceneSetup
    {
        private const string SetupSessionKey =
            "LicenseHub.UnityDemo.ActivationUiSetup.v4";

        static LicenseHubDemoSceneSetup()
        {
            EditorApplication.delayCall += PrepareAutomaticSetup;
        }

        [MenuItem("Tools/LicenseHub/Setup Activation Demo")]
        public static void SetupActivationDemo()
        {
            if (EditorApplication.isPlayingOrWillChangePlaymode)
            {
                Debug.LogWarning("Exit Play Mode before setting up the activation demo.");
                return;
            }

            Scene scene = SceneManager.GetActiveScene();

            if (!scene.IsValid() || !scene.isLoaded)
            {
                Debug.LogError("Open the demo scene before running setup.");
                return;
            }

            if (!EnsureTextMeshProResources())
            {
                EditorApplication.delayCall += SetupActivationDemo;
                return;
            }

            LicenseHubManager manager =
                Object.FindFirstObjectByType<LicenseHubManager>();

            if (manager == null)
            {
                var managerObject = new GameObject("LicenseHub");
                manager = managerObject.AddComponent<LicenseHubManager>();
            }

            EnsureEventSystem();

            TMP_FontAsset font = AssetDatabase.LoadAssetAtPath<TMP_FontAsset>(
                "Assets/TextMesh Pro/Resources/Fonts & Materials/" +
                "LiberationSans SDF.asset");

            if (font == null)
            {
                Debug.LogError("TextMesh Pro Essentials could not be loaded.");
                return;
            }

            GameObject canvasObject = GameObject.Find("ActivationCanvas");

            if (canvasObject != null)
            {
                UpgradeExistingDemo(scene, canvasObject, font);
                return;
            }

            canvasObject = CreateCanvas();
            GameObject panelObject = CreatePanel(canvasObject.transform);

            TMP_Text title = CreateText(
                "Title",
                panelObject.transform,
                font,
                "Activate License",
                36,
                58);
            title.fontStyle = FontStyles.Bold;

            TMP_InputField licenseKeyInput = CreateInput(
                "LicenseKeyInput",
                panelObject.transform,
                font,
                "License key");

            TMP_InputField machineNameInput = CreateInput(
                "MachineNameInput",
                panelObject.transform,
                font,
                "Machine name");

            Button activateButton = CreateButton(
                "ActivateButton",
                panelObject.transform,
                font,
                "Activate");

            TMP_Text statusText = CreateText(
                "StatusText",
                panelObject.transform,
                font,
                string.Empty,
                20,
                86);
            statusText.alignment = TextAlignmentOptions.Center;
            statusText.color = new Color32(220, 226, 236, 255);

            GameObject licensedContentRoot = CreateLicensedContent(
                canvasObject.transform,
                font,
                out TMP_Text licensedStatusText);

            ActivationPanel activationPanel =
                panelObject.AddComponent<ActivationPanel>();

            var serializedPanel = new SerializedObject(activationPanel);
            serializedPanel.FindProperty("licenseHubManager").objectReferenceValue = manager;
            serializedPanel.FindProperty("licenseKeyInput").objectReferenceValue =
                licenseKeyInput;
            serializedPanel.FindProperty("machineNameInput").objectReferenceValue =
                machineNameInput;
            serializedPanel.FindProperty("activateButton").objectReferenceValue =
                activateButton;
            serializedPanel.FindProperty("statusText").objectReferenceValue = statusText;
            serializedPanel.FindProperty("licensedContentRoot").objectReferenceValue =
                licensedContentRoot;
            serializedPanel.FindProperty("licensedStatusText").objectReferenceValue =
                licensedStatusText;
            serializedPanel.ApplyModifiedPropertiesWithoutUndo();

            EditorSceneManager.MarkSceneDirty(scene);
            EditorSceneManager.SaveScene(scene);
            Selection.activeGameObject = panelObject;

            Debug.Log("LicenseHub activation UI created and scene saved.");
        }

        private static void UpgradeExistingDemo(
            Scene scene,
            GameObject canvasObject,
            TMP_FontAsset font)
        {
            ActivationPanel activationPanel =
                canvasObject.GetComponentInChildren<ActivationPanel>(true);

            if (activationPanel == null)
            {
                Debug.LogError(
                    "The ActivationCanvas does not contain an ActivationPanel.");
                return;
            }

            Transform existingContent =
                canvasObject.transform.Find("LicensedContent");

            GameObject licensedContentRoot;
            TMP_Text licensedStatusText;

            if (existingContent == null)
            {
                licensedContentRoot = CreateLicensedContent(
                    canvasObject.transform,
                    font,
                    out licensedStatusText);
            }
            else
            {
                licensedContentRoot = existingContent.gameObject;
                Transform statusTransform =
                    existingContent.Find("LicensedStatusText");
                licensedStatusText =
                    statusTransform == null
                        ? null
                        : statusTransform.GetComponent<TMP_Text>();
            }

            if (licensedStatusText == null)
            {
                Debug.LogError(
                    "The licensed content does not contain its status text.");
                return;
            }

            EnsureCloseApplicationButton(
                licensedContentRoot.transform,
                font);

            licensedContentRoot.SetActive(false);

            var serializedPanel = new SerializedObject(activationPanel);
            serializedPanel.FindProperty("licensedContentRoot").objectReferenceValue =
                licensedContentRoot;
            serializedPanel.FindProperty("licensedStatusText").objectReferenceValue =
                licensedStatusText;
            serializedPanel.ApplyModifiedPropertiesWithoutUndo();

            EditorSceneManager.MarkSceneDirty(scene);
            EditorSceneManager.SaveScene(scene);
            Selection.activeGameObject = licensedContentRoot;

            Debug.Log("LicenseHub licensed-content gate configured and scene saved.");
        }

        private static void PrepareAutomaticSetup()
        {
            if (SessionState.GetBool(SetupSessionKey, false))
            {
                return;
            }

            if (EditorApplication.isPlayingOrWillChangePlaymode)
            {
                EditorApplication.playModeStateChanged -= HandlePlayModeStateChanged;
                EditorApplication.playModeStateChanged += HandlePlayModeStateChanged;
                EditorApplication.isPlaying = false;
                return;
            }

            SetupActivationDemo();

            if (GameObject.Find("ActivationCanvas") != null)
            {
                SessionState.SetBool(SetupSessionKey, true);
            }
        }

        private static void HandlePlayModeStateChanged(
            PlayModeStateChange stateChange)
        {
            if (stateChange != PlayModeStateChange.EnteredEditMode)
            {
                return;
            }

            EditorApplication.playModeStateChanged -= HandlePlayModeStateChanged;
            EditorApplication.delayCall += PrepareAutomaticSetup;
        }

        private static bool EnsureTextMeshProResources()
        {
            const string settingsPath =
                "Assets/TextMesh Pro/Resources/TMP Settings.asset";

            if (AssetDatabase.LoadAssetAtPath<TMP_Settings>(settingsPath) != null)
            {
                return true;
            }

            TMP_PackageResourceImporter.ImportResources(
                importEssentials: true,
                importExamples: false,
                interactive: false);
            AssetDatabase.Refresh();
            return false;
        }

        private static void EnsureEventSystem()
        {
            if (Object.FindFirstObjectByType<EventSystem>() != null)
            {
                return;
            }

            var eventSystemObject = new GameObject(
                "EventSystem",
                typeof(EventSystem),
                typeof(InputSystemUIInputModule));
            eventSystemObject.transform.SetAsLastSibling();
        }

        private static GameObject CreateCanvas()
        {
            var canvasObject = new GameObject(
                "ActivationCanvas",
                typeof(RectTransform),
                typeof(Canvas),
                typeof(CanvasScaler),
                typeof(GraphicRaycaster));

            Canvas canvas = canvasObject.GetComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;

            CanvasScaler scaler = canvasObject.GetComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1920, 1080);
            scaler.matchWidthOrHeight = 0.5f;

            return canvasObject;
        }

        private static GameObject CreatePanel(Transform parent)
        {
            GameObject panel = CreateUiObject("ActivationPanel", parent);
            RectTransform rect = panel.GetComponent<RectTransform>();
            rect.anchorMin = new Vector2(0.5f, 0.5f);
            rect.anchorMax = new Vector2(0.5f, 0.5f);
            rect.pivot = new Vector2(0.5f, 0.5f);
            rect.sizeDelta = new Vector2(680, 430);

            Image image = panel.AddComponent<Image>();
            image.color = new Color32(24, 31, 44, 245);

            VerticalLayoutGroup layout = panel.AddComponent<VerticalLayoutGroup>();
            layout.padding = new RectOffset(40, 40, 32, 32);
            layout.spacing = 18;
            layout.childAlignment = TextAnchor.UpperCenter;
            layout.childControlWidth = true;
            layout.childControlHeight = false;
            layout.childForceExpandWidth = true;
            layout.childForceExpandHeight = false;

            return panel;
        }

        private static GameObject CreateLicensedContent(
            Transform parent,
            TMP_FontAsset font,
            out TMP_Text statusText)
        {
            GameObject content = CreateUiObject("LicensedContent", parent);
            RectTransform rect = content.GetComponent<RectTransform>();
            Stretch(rect, 0, 0, 0, 0);

            Image background = content.AddComponent<Image>();
            background.color = new Color32(15, 23, 42, 255);

            VerticalLayoutGroup layout =
                content.AddComponent<VerticalLayoutGroup>();
            layout.padding = new RectOffset(120, 120, 260, 220);
            layout.spacing = 24;
            layout.childAlignment = TextAnchor.MiddleCenter;
            layout.childControlWidth = true;
            layout.childControlHeight = false;
            layout.childForceExpandWidth = true;
            layout.childForceExpandHeight = false;

            TMP_Text title = CreateText(
                "LicensedTitle",
                content.transform,
                font,
                "Protected content unlocked",
                44,
                72);
            title.fontStyle = FontStyles.Bold;
            title.color = new Color32(134, 239, 172, 255);

            TMP_Text description = CreateText(
                "LicensedDescription",
                content.transform,
                font,
                "LicenseHub validated this machine. The game can now start.",
                24,
                58);
            description.color = new Color32(226, 232, 240, 255);

            statusText = CreateText(
                "LicensedStatusText",
                content.transform,
                font,
                string.Empty,
                18,
                100);
            statusText.color = new Color32(148, 163, 184, 255);

            EnsureCloseApplicationButton(content.transform, font);

            content.SetActive(false);
            return content;
        }

        private static void EnsureCloseApplicationButton(
            Transform parent,
            TMP_FontAsset font)
        {
            VerticalLayoutGroup parentLayout =
                parent.GetComponent<VerticalLayoutGroup>();

            if (parentLayout != null)
            {
                parentLayout.childForceExpandWidth = false;
            }

            Transform existingButton =
                parent.Find("CloseApplicationButton");

            if (existingButton != null)
            {
                if (existingButton.GetComponent<QuitApplicationButton>() == null)
                {
                    existingButton.gameObject.AddComponent<QuitApplicationButton>();
                }

                return;
            }

            Button closeButton = CreateButton(
                "CloseApplicationButton",
                parent,
                font,
                "Close application");

            closeButton.GetComponent<Image>().color =
                new Color32(220, 38, 38, 255);

            LayoutElement layout =
                closeButton.GetComponent<LayoutElement>();
            layout.preferredWidth = 320;

            closeButton.gameObject.AddComponent<QuitApplicationButton>();
        }

        private static TMP_InputField CreateInput(
            string name,
            Transform parent,
            TMP_FontAsset font,
            string placeholderText)
        {
            GameObject inputObject = CreateUiObject(name, parent);
            Image background = inputObject.AddComponent<Image>();
            background.color = new Color32(245, 247, 250, 255);

            var layout = inputObject.AddComponent<LayoutElement>();
            layout.preferredHeight = 58;

            GameObject viewportObject = CreateUiObject("Text Area", inputObject.transform);
            RectTransform viewportRect = viewportObject.GetComponent<RectTransform>();
            Stretch(viewportRect, 14, 14, 6, 6);
            viewportObject.AddComponent<RectMask2D>();

            TMP_Text placeholder = CreateInputText(
                "Placeholder",
                viewportObject.transform,
                font,
                placeholderText,
                new Color32(110, 119, 133, 255));
            placeholder.fontStyle = FontStyles.Italic;

            TMP_Text valueText = CreateInputText(
                "Text",
                viewportObject.transform,
                font,
                string.Empty,
                new Color32(25, 31, 42, 255));

            TMP_InputField input = inputObject.AddComponent<TMP_InputField>();
            input.textViewport = viewportRect;
            input.textComponent = valueText;
            input.placeholder = placeholder;
            input.lineType = TMP_InputField.LineType.SingleLine;
            input.targetGraphic = background;

            return input;
        }

        private static TMP_Text CreateInputText(
            string name,
            Transform parent,
            TMP_FontAsset font,
            string value,
            Color color)
        {
            GameObject textObject = CreateUiObject(name, parent);
            RectTransform rect = textObject.GetComponent<RectTransform>();
            Stretch(rect, 0, 0, 0, 0);

            TMP_Text text = textObject.AddComponent<TextMeshProUGUI>();
            text.font = font;
            text.fontSize = 22;
            text.text = value;
            text.color = color;
            text.alignment = TextAlignmentOptions.MidlineLeft;
            text.raycastTarget = false;

            return text;
        }

        private static Button CreateButton(
            string name,
            Transform parent,
            TMP_FontAsset font,
            string label)
        {
            GameObject buttonObject = CreateUiObject(name, parent);
            Image image = buttonObject.AddComponent<Image>();
            image.color = new Color32(37, 99, 235, 255);

            var layout = buttonObject.AddComponent<LayoutElement>();
            layout.preferredHeight = 58;

            Button button = buttonObject.AddComponent<Button>();
            button.targetGraphic = image;

            ColorBlock colors = button.colors;
            colors.normalColor = Color.white;
            colors.highlightedColor = new Color32(220, 230, 255, 255);
            colors.pressedColor = new Color32(180, 200, 245, 255);
            colors.disabledColor = new Color32(125, 135, 155, 255);
            button.colors = colors;

            GameObject labelObject = CreateUiObject("Text", buttonObject.transform);
            RectTransform labelRect = labelObject.GetComponent<RectTransform>();
            Stretch(labelRect, 0, 0, 0, 0);

            TMP_Text buttonText = labelObject.AddComponent<TextMeshProUGUI>();
            buttonText.font = font;
            buttonText.fontSize = 22;
            buttonText.fontStyle = FontStyles.Bold;
            buttonText.text = label;
            buttonText.color = Color.white;
            buttonText.alignment = TextAlignmentOptions.Center;
            buttonText.raycastTarget = false;

            return button;
        }

        private static TMP_Text CreateText(
            string name,
            Transform parent,
            TMP_FontAsset font,
            string value,
            float fontSize,
            float preferredHeight)
        {
            GameObject textObject = CreateUiObject(name, parent);
            var layout = textObject.AddComponent<LayoutElement>();
            layout.preferredHeight = preferredHeight;

            TMP_Text text = textObject.AddComponent<TextMeshProUGUI>();
            text.font = font;
            text.fontSize = fontSize;
            text.text = value;
            text.color = Color.white;
            text.alignment = TextAlignmentOptions.Center;
            text.raycastTarget = false;

            return text;
        }

        private static GameObject CreateUiObject(string name, Transform parent)
        {
            var gameObject = new GameObject(name, typeof(RectTransform));
            gameObject.transform.SetParent(parent, false);
            return gameObject;
        }

        private static void Stretch(
            RectTransform rect,
            float left,
            float right,
            float top,
            float bottom)
        {
            rect.anchorMin = Vector2.zero;
            rect.anchorMax = Vector2.one;
            rect.offsetMin = new Vector2(left, bottom);
            rect.offsetMax = new Vector2(-right, -top);
        }
    }
}
