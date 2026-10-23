#include "DateEngine.hpp"
#include <cstdio>

namespace finpay {

static bool isLeapYear(int year) {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
}

int DateEngine::daysInMonth(int year, int month) {
    static const int days[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    if (month < 1 || month > 12) return 30;
    if (month == 2 && isLeapYear(year)) return 29;
    return days[month - 1];
}

// Algoritmo de Tomohiko Sakamoto para determinar o dia da semana (0 = Domingo, 6 = Sábado)
static int dayOfWeek(int y, int m, int d) {
    static const int t[] = { 0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4 };
    if (m < 3) y -= 1;
    return (y + y / 4 - y / 100 + y / 400 + t[m - 1] + d) % 7;
}

bool DateEngine::isWeekend(int year, int month, int day) {
    int dow = dayOfWeek(year, month, day);
    return dow == 0 || dow == 6;
}

// Converte (ano, mês, dia) para número de dias desde o início do ano (1 a 366)
static int dayOfYear(int year, int month, int day) {
    int doy = day;
    for (int m = 1; m < month; ++m) {
        doy += DateEngine::daysInMonth(year, m);
    }
    return doy;
}

bool DateEngine::isNationalHoliday(int year, int month, int day) {
    // 1. Feriados Nacionais Fixos no Brasil
    if (month == 1  && day == 1)  return true; // Confraternização Universal
    if (month == 4  && day == 21) return true; // Tiradentes
    if (month == 5  && day == 1)  return true; // Dia do Trabalhador
    if (month == 9  && day == 7)  return true; // Independência do Brasil
    if (month == 10 && day == 12) return true; // Nossa Senhora Aparecida
    if (month == 11 && day == 2)  return true; // Finados
    if (month == 11 && day == 15) return true; // Proclamação da República
    if (month == 11 && day == 20) return true; // Dia Nacional de Zumbi e da Consciência Negra
    if (month == 12 && day == 25) return true; // Natal

    // 2. Algoritmo de Meeus/Jones/Butcher para calcular o Domingo de Páscoa
    int a = year % 19;
    int b = year / 100;
    int c = year % 100;
    int d = b / 4;
    int e = b % 4;
    int f = (b + 8) / 25;
    int g = (b - f + 1) / 3;
    int h = (19 * a + b - d - g + 15) % 30;
    int i = c / 4;
    int k = c % 4;
    int l = (32 + 2 * e + 2 * i - h - k) % 7;
    int m = (a + 11 * h + 22 * l) / 451;
    int easterMonth = (h + l - 7 * m + 114) / 31;
    int easterDay = ((h + l - 7 * m + 114) % 31) + 1;

    int easterDoy = dayOfYear(year, easterMonth, easterDay);
    int targetDoy = dayOfYear(year, month, day);

    // Carnaval (Segunda e Terça: -48 e -47 dias antes da Páscoa)
    if (targetDoy == easterDoy - 48 || targetDoy == easterDoy - 47) return true;
    // Sexta-feira Santa (-2 dias antes da Páscoa)
    if (targetDoy == easterDoy - 2) return true;
    // Corpus Christi (+60 dias após a Páscoa)
    if (targetDoy == easterDoy + 60) return true;

    return false;
}

bool DateEngine::isBusinessDay(int year, int month, int day) {
    return !isWeekend(year, month, day) && !isNationalHoliday(year, month, day);
}

static std::string formatDateBR(int day, int month, int year) {
    char buf[16];
    std::snprintf(buf, sizeof(buf), "%02d/%02d/%04d", day, month, year);
    return std::string(buf);
}

std::string DateEngine::getLastBusinessDayFormatted(int year, int month) {
    int d = daysInMonth(year, month);
    while (d > 1 && !isBusinessDay(year, month, d)) {
        --d;
    }
    return formatDateBR(d, month, year);
}

std::string DateEngine::getFifthBusinessDayFormatted(int year, int month) {
    int count = 0;
    int maxD = daysInMonth(year, month);
    for (int d = 1; d <= maxD; ++d) {
        if (isBusinessDay(year, month, d)) {
            ++count;
            if (count == 5) {
                return formatDateBR(d, month, year);
            }
        }
    }
    return formatDateBR(5, month, year);
}

} // namespace finpay
