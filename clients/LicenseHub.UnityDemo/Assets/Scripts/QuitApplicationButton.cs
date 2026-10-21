using UnityEngine;
using UnityEngine.UI;

namespace LicenseHub.UnityDemo
{
    [RequireComponent(typeof(Button))]
    public sealed class QuitApplicationButton : MonoBehaviour
    {
        private Button button;

        private void OnEnable()
        {
            button = GetComponent<Button>();
            button.onClick.AddListener(QuitApplication);
        }

        private void OnDisable()
        {
            if (button != null)
            {
                button.onClick.RemoveListener(QuitApplication);
            }
        }

        private static void QuitApplication()
        {
#if UNITY_EDITOR
            UnityEditor.EditorApplication.isPlaying = false;
#else
            Application.Quit();
#endif
        }
    }
}
