package com.ats_connection.asa.orchestrator.helper;

public class Utils {
	
    /**
     * Obtiene el codigo de area que esta en el LOCI.
     *  
     * El LOCI puede ser de 7, 9 (LTE) o de 11 bytes. 
     * Ejemplos: LOCI=680FA42027F4011C2BFF00 11 bytes, LOCI=27F4011C2BFF00 7 bytes 
     * Descartamos los primeros 14 digitos, nos queda: 1C2BFF00 
     * Luego, nos quedamos con los siguientes 4 digitos: 1C2B 
     * Ese valor esta en hexa, lo pasamos a decimal: 7211 
     * 72 es el MNC (Mobile National Code) y 11 es el codigo de area (LAC) 
     * Tenemos que retornar 11 en este caso.
     * 
     * @param loci
     * @return
     */
    public static String getAreaCodeFromLoci(String loci, int dataLen, int dataPos) {
        String areaCode = null;
        String aux = null;

        if (loci != null) {
            int lociLen = loci.length();
            switch (lociLen) {
            case 14: // Ej: 27F401 1C2B FF00
            case 18: // Ej: 27F401 9CAF 9596006F  LTE
                aux = loci.substring(6, 10);
                break;
            case 22:
                // Ej: 680FA42027F401 1C2B FF00
                aux = loci.substring(14, 18);
                break;
            default:
                LogHelper.webLog.error("Invalid LOCI length:" + lociLen);
            }

       if (aux != null) {
        // Lo pasamos de hexa a decimal
        String data = String.format("%05d", Integer.decode("0x"+aux));
        // Nos quedamos con los digitos segun los parametros
        if (data.length() >= dataPos+dataLen)
          areaCode = data.substring(dataPos, dataPos+dataLen);
       }
      }

      return areaCode;
     }
}
