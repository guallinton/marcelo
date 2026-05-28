#ifndef UNICODE
#define UNICODE
#endif
#ifndef _UNICODE
#define _UNICODE
#endif

#include <windows.h>
#include <wchar.h>

static void executable_directory(wchar_t *buffer, DWORD length) {
    DWORD written = GetModuleFileNameW(NULL, buffer, length);
    if (written == 0 || written >= length) {
        buffer[0] = L'\0';
        return;
    }
    for (DWORD i = written; i > 0; i--) {
        if (buffer[i] == L'\\' || buffer[i] == L'/') {
            buffer[i] = L'\0';
            return;
        }
    }
}

int WINAPI wWinMain(HINSTANCE instance, HINSTANCE previousInstance, PWSTR commandLine, int showCommand) {
    (void) instance;
    (void) previousInstance;
    (void) commandLine;
    (void) showCommand;

    wchar_t appDirectory[MAX_PATH];
    executable_directory(appDirectory, MAX_PATH);
    if (appDirectory[0] == L'\0') {
        MessageBoxW(NULL, L"No se pudo ubicar la carpeta de la aplicacion.", L"Agenda Quimioterapia", MB_ICONERROR);
        return 1;
    }

    SetCurrentDirectoryW(appDirectory);

    wchar_t javaCommand[4096];
    _snwprintf(
            javaCommand,
            4096,
            L"javaw.exe -cp \"agenda-quimioterapia-1.3.0.jar;lib\\*\" com.oncologia.agenda.AppLauncher"
    );
    javaCommand[4095] = L'\0';

    STARTUPINFOW startupInfo;
    PROCESS_INFORMATION processInfo;
    ZeroMemory(&startupInfo, sizeof(startupInfo));
    ZeroMemory(&processInfo, sizeof(processInfo));
    startupInfo.cb = sizeof(startupInfo);

    BOOL started = CreateProcessW(
            NULL,
            javaCommand,
            NULL,
            NULL,
            FALSE,
            0,
            NULL,
            appDirectory,
            &startupInfo,
            &processInfo
    );

    if (!started) {
        MessageBoxW(
                NULL,
                L"No se pudo iniciar Java. Instale Java 17 o superior y vuelva a abrir AgendaQuimioterapia.exe.",
                L"Agenda Quimioterapia",
                MB_ICONERROR
        );
        return 1;
    }

    CloseHandle(processInfo.hProcess);
    CloseHandle(processInfo.hThread);
    return 0;
}
