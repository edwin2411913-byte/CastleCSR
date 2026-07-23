@echo off
REM Cargar variables desde .env

if exist .env (
    for /f "delims== tokens=1,2" %%A in (.env) do (
        if not "%%A"=="" (
            if not "%%A:~0,1%"=="#" (
                set %%A=%%B
            )
        )
    )
    echo OK - Variables de entorno cargadas desde .env
) else (
    echo ERROR - Archivo .env no encontrado
    exit /b 1
)