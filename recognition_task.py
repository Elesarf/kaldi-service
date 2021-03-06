#!/usr/bin/python
import os
import time
import argparse
import glob
import logging
import csv
import json
import pandas
from pathlib import Path
from tqdm import tqdm
from multiprocessing import Pool, cpu_count
from tools import data_preparator, segmenter, recognizer, transcriptions_parser
from tools.utils import make_ass, delete_folder, make_wav_scp, create_logger, prepare_wav

def start_pipeline(wav):
    """
    Запуск пайплайна распознавания речи
    
    Аргументы:
        wav: путь к .WAV файлу аудио
    """
    wav = prepare_wav(wav)
    wav_name = Path(wav).name
    wav_stem = Path(wav).stem
    temp = str(Path(TEMP_DIR) / wav_stem)
    os.makedirs(temp, exist_ok=True)
    wav_scp = str(Path(temp) / 'wav.scp')
    make_wav_scp(wav, wav_scp)
    
    def terminate_pipeline(is_error, message):
        if is_error:
            LOGGER.error(message)
            os.rename(wav, str(ERROR_DIR / wav_name))
        try:
            delete_folder(temp)
        except:
            LOGGER.error("Не удалось удалить временные файлы для '{}'".format(wav_name))

    try:
        LOGGER.info("Запуск сегментации файла '{}'".format(wav_name))
        segm = segmenter.Segmenter(wav_scp, SEGM_MODEL, SEGM_POST, SEGM_CONF, temp)
        segments = segm.segment()
        LOGGER.info("Завершение сегментации файла '{}'".format(wav_name))
    except:
        terminate_pipeline(True, "Не удалось выполнить сегментацию файла '{}'".format(wav_name))
        return
    if os.stat(segments).st_size == 0:
        terminate_pipeline(True, "В файле '{}' отсутствуют сегменты".format(wav_name))
        return

    try:
        LOGGER.info("Запуск извлечения сегментов из файла '{}'".format(wav_name))
        wav_segments_scp, utt2spk, spk2utt = segm.extract_segments(segments)
        LOGGER.info("Завершение извлечения сегментов из файла '{}'".format(wav_name))
    except:
        terminate_pipeline(True, "Не удалось извлечь сегменты из файла '{}'".format(wav_name))
        return            
    try:
        LOGGER.info("Запуск распознавания файла '{}'".format(wav_name))
        rec = recognizer.Recognizer(wav_segments_scp, REC_MODEL, REC_GRAPH, REC_WORDS, REC_CONF, REC_ICONF, spk2utt, temp)
        transcriptions = rec.recognize(wav_stem)
        LOGGER.info("Завершение распознавания файла '{}'".format(wav_name))
    except:
        terminate_pipeline(True, "Не удалось выполнить распознавание файла '{}'".format(wav_name))
        return
    try:
        LOGGER.info("Запуск формирования субтитров для файла '{}'".format(wav_name))
        ass = str(OUTPUT_DIR / str('ass/' + wav_stem + '.ass'))
        make_ass(wav_name, segments, transcriptions, utt2spk, ass)
        LOGGER.info("Завершение формирования субтитров для файла '{}'".format(wav_name))
    except:
        terminate_pipeline(True, "Не удалось сформировать субтитры для файла '{}'".format(wav_name))
        return
    try:
        LOGGER.info("Запуск парсинга транскрибации для файла '{}'".format(wav_name))
        pars = transcriptions_parser.TranscriptionsParser(
            str(OUTPUT_DIR / 'ass'),
            OUTPUT_DIR,
            LOGGER.handlers[0].baseFilename if IS_LOG else '', 
            1, 
            1, 
            CSV)
        pars.process_batch_files([ass])
        LOGGER.info("Завершение парсинга транскрибации для файла '{}'".format(wav_name))
    except:
        terminate_pipeline(True, "Не удалось распарсить транскрибацию файла '{}'".format(wav_name))
        return
    try:
        readCSV = pandas.read_csv(CSV)
        readCSV.to_json(str(OUTPUT_DIR / 'result.json'), orient="records")
    except:
        terminate_pipeline(True, "Не удалось сериализировать результат распознавания '{}'".format(wav_name))
        return

    terminate_pipeline(False, None)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Запуск процедуры распознавания речи')
    parser.add_argument('wav', metavar='WAV', help='Путь к .WAV файлам аудио')
    parser.add_argument('output', metavar='OUT', help='Путь к директории с результатами распознавания')
    parser.add_argument('-rm', '--rec_model', help='Путь к .MDL файлу модели распознавания')
    parser.add_argument('-rg', '--rec_graph', help='Путь к .FST файлу общего графа распознавания')
    parser.add_argument('-rw', '--rec_words', help='Путь к .TXT файлу текстового корпуса')
    parser.add_argument('-rc', '--rec_conf', help='Путь к .CONF конфигурационному файлу распознавания')
    parser.add_argument('-ri', '--rec_iconf', help='Путь к .CONF конфигурационному файлу векторного экстрактора')
    parser.add_argument('-sm', '--segm_model', help='Путь к .RAW файлу модели сегментации')
    parser.add_argument('-sc', '--segm_conf', help='Путь к .CONF конфигурационному файлу сегментации')
    parser.add_argument('-sp', '--segm_post', help='Путь к .VEC файлу апостериорных вероятностей сегментации')
    parser.add_argument('-p', '--processes', default=None, type=int, help='Количество процессов для обработки файлов')
    parser.add_argument('-dw', '--delete_wav', dest='delete_wav', action='store_true', help='Удалять .WAV файлы после распознавания')

    args = parser.parse_args()

    WAV_FILE = Path(args.wav)
    OUTPUT_DIR = Path(args.output)
    REC_MODEL = args.rec_model or 'model/final.mdl'
    REC_GRAPH = args.rec_graph or 'model/HCLG.fst'
    REC_WORDS = args.rec_words or 'model/words.txt'
    REC_CONF = args.rec_conf or 'model/conf/mfcc.conf'
    REC_ICONF = args.rec_iconf or 'model/conf/ivector_extractor.conf'
    SEGM_MODEL = args.segm_model or 'model/final.raw'
    SEGM_CONF = args.segm_conf or 'model/conf/mfcc_hires.conf'
    SEGM_POST = args.segm_post or 'model/conf/post_output.vec'
    PROCESSES = args.processes or cpu_count()
    IS_LOG = 'yes'
    
    wavs = glob.glob(str(WAV_FILE))
    
    prep = data_preparator.DataPreparator(args.wav, str(OUTPUT_DIR), IS_LOG)
    LOG_DIR, TEMP_DIR, ASS_DIR, ERROR_DIR = prep.create_directories()
   
    if IS_LOG:
        try:
            log_name = str(LOG_DIR / 'recognition.log')
            LOGGER = create_logger('logger', 'file', logging.DEBUG, log_name)
        except:
            raise Exception("Не удалось создать лог-файл")
    else:
        LOGGER = create_logger('logger', 'stream', logging.INFO)

    try:
        CSV = str(OUTPUT_DIR / str('transcriptions_' + time.strftime('%Y%m%d-%H%M%S') + '.csv'))
        with open(CSV, 'w') as f:
            writer = csv.writer(f)
            writer.writerow(['AudioFile', 'Start', 'End', 'Name', 'Text'])
    except:
        raise Exception("Не удалось создать результирующий .CSV-файл")

    LOGGER.info("Starting rspeech recognition")
    if wavs:
        start_pipeline(wavs[0])
    LOGGER.info("Finishing speech recognition")
