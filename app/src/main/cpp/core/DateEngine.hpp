#ifndef FINPAY_DATE_ENGINE_HPP
#define FINPAY_DATE_ENGINE_HPP

#include <string>

namespace finpay {

/**
 * Motor de calendário bancário brasileiro em C++ puro.
 * Calcula feriados nacionais fixos e móveis (Páscoa, Carnaval, Sexta-feira Santa e Corpus Christi)
 * usando o algoritmo astronômico de Meeus/Jones/Butcher e determina dias úteis bancários.
 */
class DateEngine {
public:
    /**
     * Verifica se uma data (ano, mês 1-12, dia 1-31) é fim de semana (sábado ou domingo).
     */
    static bool isWeekend(int year, int month, int day);

    /**
     * Verifica se uma data é feriado nacional brasileiro (fixo ou móvel).
     */
    static bool isNationalHoliday(int year, int month, int day);

    /**
     * Verifica se uma data é dia útil bancário no Brasil.
     */
    static bool isBusinessDay(int year, int month, int day);

    /**
     * Retorna a quantidade de dias de um mês (considerando anos bissextos).
     */
    static int daysInMonth(int year, int month);

    /**
     * Retorna a data do último dia útil do mês no formato "DD/MM/YYYY" (ex: "30/10/2026").
     */
    static std::string getLastBusinessDayFormatted(int year, int month);

    /**
     * Retorna a data do 5º dia útil do mês no formato "DD/MM/YYYY".
     */
    static std::string getFifthBusinessDayFormatted(int year, int month);
};

} // namespace finpay

#endif // FINPAY_DATE_ENGINE_HPP
